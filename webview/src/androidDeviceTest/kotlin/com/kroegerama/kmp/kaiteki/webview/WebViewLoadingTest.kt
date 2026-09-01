package com.kroegerama.kmp.kaiteki.webview

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/** Loading state timing and painting within bounds, against a server that holds its response until released. */
class WebViewLoadingTest {

    @Test
    fun loadingIsReportedWhileTheResponseIsPending() = HoldingServer().use { server ->
        runWebView(initialUrl = "${server.base}/held") { controller, _, _ ->
            // Nothing has been received yet, so the only source for this is the pre-commit progress estimate.
            assertTrue("no Loading while the response is pending", await { controller.loadingState is LoadingState.Loading })
            assertEquals(LoadingState.Loading(0.1f), controller.loadingState)
            server.release()
            assertTrue(await { controller.loadingState == LoadingState.Finished })
        }
    }

    @Test
    fun cancelledNavigationDoesNotReportLoading() = HoldingServer().use { server ->
        server.release()
        runWebView(initialUrl = "${server.base}/external") { controller, states, uriHandler ->
            assertTrue(await { controller.loadingState == LoadingState.Finished })
            assertTrue("mailto: never reached the UriHandler", await { uriHandler.opened.isNotEmpty() })
            // Chromium reports start and completion for the cancelled navigation; give both time to arrive.
            Thread.sleep(500)
            val afterFirstFinish = states.toList().dropWhile { it != LoadingState.Finished }
            assertEquals(listOf(LoadingState.Finished), afterFirstFinish.distinct())
        }
    }

    @Test
    fun paintsOnlyWithinItsBounds() = HoldingServer().use { server ->
        val headerCenter = AtomicReference<IntOffset>()
        runWebView(
            initialUrl = "${server.base}/held",
            header = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Red)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionOnScreen()
                            headerCenter.set(IntOffset((pos.x + coords.size.width / 2).roundToInt(), (pos.y + coords.size.height / 2).roundToInt()))
                        },
                )
            },
        ) { controller, _, _ ->
            assertTrue(await { controller.loadingState is LoadingState.Loading })
            assertTrue(await { headerCenter.get() != null })
            // Before the first committed frame, Chromium fills the whole canvas clip with its background color;
            // without a clip the web view would cover the sibling above it.
            Thread.sleep(300)
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val center = headerCenter.get()
            val pixel = screenshot.getPixel(center.x, center.y)
            screenshot.recycle()
            assertEquals("header pixel is %08X".format(pixel), 0xFFFF0000.toInt(), pixel)
            server.release()
        }
    }

    private fun runWebView(
        initialUrl: String,
        header: @androidx.compose.runtime.Composable () -> Unit = {},
        block: (WebViewController, List<LoadingState>, RecordingUriHandler) -> Unit,
    ) {
        val uriHandler = RecordingUriHandler()
        val controller = AtomicReference<WebViewController>()
        val states = Collections.synchronizedList(mutableListOf<LoadingState>())
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    val c = rememberWebViewController(initialUrl)
                    controller.set(c)
                    LaunchedEffect(c) {
                        snapshotFlow { c.loadingState }.collect { states += it }
                    }
                    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            header()
                            WebView(controller = c, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
            assertTrue("the web view never composed", await { controller.get() != null })
            block(controller.get(), states, uriHandler)
        }
    }

    private fun await(timeoutMillis: Long = 10_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(25)
        }
        return condition()
    }
}

private const val MAILTO = "mailto:someone@example.com"

/** Serves `/held` only after [release]; everything else answers immediately. */
private class HoldingServer : Closeable {

    private val socket = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
    private val gate = CountDownLatch(1)

    val base: String get() = "http://127.0.0.1:${socket.localPort}"

    init {
        thread(isDaemon = true) {
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (_: Exception) {
                    return@thread
                }
                thread(isDaemon = true) {
                    try {
                        client.use(::respond)
                    } catch (_: IOException) {
                        // The web view closes speculative connections; nothing to serve then.
                    }
                }
            }
        }
    }

    fun release() {
        gate.countDown()
    }

    private fun respond(client: Socket) {
        val requestLine = BufferedReader(InputStreamReader(client.getInputStream())).readLine() ?: return
        val path = requestLine.split(" ").getOrElse(1) { "/" }
        if (path.startsWith("/held")) gate.await()
        // Delayed, so the cancelled navigation starts from a finished page rather than during the initial load.
        val extra = if (path.startsWith("/external")) "<script>setTimeout(function(){location.href='$MAILTO'},300);</script>" else ""
        val bytes = "<html><body>$path$extra</body></html>".toByteArray()
        val out = client.getOutputStream()
        out.write(
            ("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray()
        )
        out.write(bytes)
        out.flush()
    }

    override fun close() {
        gate.countDown()
        socket.close()
    }
}
