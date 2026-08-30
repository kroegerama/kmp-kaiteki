package com.kroegerama.kmp.kaiteki.webview

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/** HTTP error reporting and external scheme handling, driven through the public [WebView] API. */
class WebViewNavigationTest {

    @Test
    fun mainFrameHttpErrorIsReported() = TinyServer().use { server ->
        runWebView(initialUrl = "${server.base}/notfound") { controller, _ ->
            assertTrue("no http error reported", await { controller.lastHttpError != null })
            assertEquals(404, controller.lastHttpError?.statusCode)
            assertEquals("${server.base}/notfound", controller.lastHttpError?.url)
            // The page still loads and renders the server's error body, so this is not a load failure.
            assertNull(controller.lastError)
            assertTrue(await { controller.loadingState == LoadingState.Finished })
        }
    }

    @Test
    fun subresourceHttpErrorIsIgnored() = TinyServer().use { server ->
        runWebView(initialUrl = "${server.base}/withBadImage") { controller, _ ->
            assertTrue(await { controller.loadingState == LoadingState.Finished })
            assertNull(controller.lastHttpError)
        }
    }

    @Test
    fun httpErrorIsClearedOnNextNavigation() = TinyServer().use { server ->
        runWebView(initialUrl = "${server.base}/notfound") { controller, _ ->
            assertTrue(await { controller.lastHttpError != null })
            controller.loadUrl("${server.base}/ok")
            assertTrue("stale http error survived the next load", await { controller.lastHttpError == null })
            assertTrue(await { controller.loadingState == LoadingState.Finished })
        }
    }

    @Test
    fun externalSchemeIsHandedToUriHandler() = TinyServer().use { server ->
        runWebView(initialUrl = "${server.base}/external") { controller, uriHandler ->
            assertTrue("mailto: never reached the UriHandler", await { uriHandler.opened.isNotEmpty() })
            assertEquals(listOf(MAILTO), uriHandler.opened.toList())
            // Cancelling the navigation must not leave an error page behind.
            assertNull(controller.lastError)
            assertEquals("${server.base}/external", controller.currentUrl)
        }
    }

    @Test
    fun externalSchemeIsLeftToTheWebViewWhenDisabled() = TinyServer().use { server ->
        val settings = WebViewSettings(openExternalSchemes = false)
        runWebView(initialUrl = "${server.base}/external", settings = settings) { controller, uriHandler ->
            assertTrue("no load failure for the unsupported scheme", await { controller.lastError != null })
            assertEquals(MAILTO, controller.lastError?.failingUrl)
            assertTrue(uriHandler.opened.isEmpty())
        }
    }

    @Test
    fun programmaticExternalLoadIsNotRedirected() = TinyServer().use { server ->
        runWebView(initialUrl = "${server.base}/ok") { controller, uriHandler ->
            assertTrue(await { controller.loadingState == LoadingState.Finished })
            controller.loadUrl(MAILTO)
            assertTrue(await { controller.lastError != null })
            assertTrue(uriHandler.opened.isEmpty())
        }
    }

    private fun runWebView(
        initialUrl: String,
        settings: WebViewSettings = WebViewSettings(),
        block: (WebViewController, RecordingUriHandler) -> Unit,
    ) {
        val uriHandler = RecordingUriHandler()
        val controller = AtomicReference<WebViewController>()
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    val c = rememberWebViewController(initialUrl, settings)
                    controller.set(c)
                    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                        WebView(controller = c, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            assertTrue("the web view never composed", await { controller.get() != null })
            block(controller.get(), uriHandler)
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

private class RecordingUriHandler : UriHandler {

    val opened: MutableList<String> = Collections.synchronizedList(mutableListOf())

    override fun openUri(uri: String) {
        opened += uri
    }
}

/** Serves the fixtures over loopback, so the tests need no network. */
private class TinyServer : Closeable {

    private val socket = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))

    val base: String get() = "http://127.0.0.1:${socket.localPort}"

    init {
        thread(isDaemon = true) {
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (_: Exception) {
                    return@thread
                }
                thread(isDaemon = true) { client.use(::respond) }
            }
        }
    }

    private fun respond(client: Socket) {
        val requestLine = BufferedReader(InputStreamReader(client.getInputStream())).readLine() ?: return
        val path = requestLine.split(" ").getOrElse(1) { "/" }
        val (status, body) = when {
            path.startsWith("/notfound") -> "404 Not Found" to "<html><body>missing</body></html>"
            path.startsWith("/withBadImage") -> "200 OK" to "<html><body><img src=\"/notfound.png\"></body></html>"
            path.startsWith("/external") -> "200 OK" to "<html><body><script>location.href='$MAILTO';</script></body></html>"
            else -> "200 OK" to "<html><body>ok</body></html>"
        }
        val bytes = body.toByteArray()
        val out = client.getOutputStream()
        out.write(
            ("HTTP/1.1 $status\r\nContent-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray()
        )
        out.write(bytes)
        out.flush()
    }

    override fun close() {
        socket.close()
    }
}
