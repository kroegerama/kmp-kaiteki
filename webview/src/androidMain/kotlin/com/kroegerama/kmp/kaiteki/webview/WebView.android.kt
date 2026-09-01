package com.kroegerama.kmp.kaiteki.webview

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

public actual typealias WebViewSavedState = Bundle

@Composable
internal actual fun PlatformWebView(
    controller: WebViewController,
    modifier: Modifier,
    handleBackNavigation: Boolean,
) {
    // BackHandler requires an OnBackPressedDispatcherOwner even when disabled, so it must not be composed at all when back is not handled.
    if (handleBackNavigation) {
        BackHandler(enabled = controller.canGoBack) {
            controller.goBack()
        }
    }
    val uriHandler = LocalUriHandler.current
    // Clients, settings and command collection bind to one controller instance; recreate the native view when it changes.
    key(controller) {
        var view by remember { mutableStateOf<WebView?>(null) }
        // Bumped when the render process dies: the only recovery is to destroy the view and create a fresh one.
        var recreationKey by remember { mutableIntStateOf(0) }

        key(recreationKey) {
            val renderProcessGone = remember { mutableStateOf(false) }
            // Shared by the saver capture and the teardown stash, so an unchanged history is serialized only once.
            val stateCapture = remember { StateCapture() }
            // Compose applies node changes before dispatching release callbacks, so the replacement view's factory
            // runs before this view's onRelease: everything the replacement has to observe is recorded here instead.
            val onProcessGone: () -> Unit = {
                renderProcessGone.value = true
                controller.nativeStateProvider = null
                view = null
                // A view whose render process died has no capturable state.
                controller.stashState(null)
                // The dead view's history is gone; the replacement reports its own once a page loads.
                controller.title = null
                controller.canGoBack = false
                controller.canGoForward = false
                if (controller.crashAutoReloadAttempted) {
                    // Repeated crash without a finished load in between: come back blank and leave recovery to the app.
                    controller.crashSuppressedGeneration = controller.contentGeneration
                } else {
                    controller.crashAutoReloadAttempted = true
                }
                recreationKey++
            }
            val client = remember { ControllerClient(controller, stateCapture, onProcessGone, uriHandler) }
            val chromeClient = remember { ControllerChromeClient(controller, client) }
            // LocalUriHandler can be replaced by a consumer at any time; the client keeps the current one.
            SideEffect { client.uriHandler = uriHandler }
            // AndroidView only turns MATCH_PARENT into an EXACTLY spec under a bounded height constraint; an unbounded one
            // (scrolling parent, wrapContentHeight) measures UNSPECIFIED, where a MATCH_PARENT WebView reports zero height and
            // never paints. WRAP_CONTENT puts Chromium in zero-layout-height mode, so the page lays out and grows the view.
            BoxWithConstraints(
                modifier = modifier.clipToBounds(),
                propagateMinConstraints = true,
            ) {
                val heightLayoutParam = if (constraints.hasBoundedHeight) MATCH_PARENT else WRAP_CONTENT
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, heightLayoutParam)
                            applySettings(controller.settings)
                            webViewClient = client
                            webChromeClient = chromeClient
                            // A skip marker from a previous view must never suppress this view's load.
                            controller.restoredGeneration = -1
                            controller.pendingRestore?.let { pending ->
                                controller.pendingRestore = null
                                // A successful restore replaces the stash-time generation's load; restoreState must run before any load.
                                // Loads issued after the stash bump the generation past it and still run, on top of the restored history.
                                if (restoreState(pending.state) != null) {
                                    controller.restoredGeneration = pending.generation
                                } else {
                                    controller.fallbackToCurrentUrl()
                                }
                            }
                            if (controller.crashSuppressedGeneration == controller.contentGeneration) {
                                controller.restoredGeneration = controller.contentGeneration
                            }
                            controller.crashSuppressedGeneration = -1
                            controller.nativeStateProvider = { stateCapture.capture(this) }
                            view = this
                        }
                    },
                    onRelease = {
                        // Cancels pending dialog handlers; their JsResults complete as cancelled, unblocking page JS.
                        chromeClient.dispose()
                        // A replacement view's factory has already installed its own provider by now; only clear what is still ours.
                        if (view === it) {
                            controller.nativeStateProvider = null
                            view = null
                        }
                        // A view whose render process died is only detached and destroyed; its bookkeeping ran in onProcessGone.
                        if (!renderProcessGone.value) {
                            // Stash state before teardown so a recreated view restores history instead of reloading (or staying blank).
                            controller.stashState(stateCapture.capture(it))
                            it.stopLoading()
                            it.webChromeClient = null
                        }
                        (it.parent as? ViewGroup)?.removeView(it)
                        it.destroy()
                    },
                    update = {
                        // setLayoutParams triggers a layout pass and a Chromium layout-mode update, so reassign only on an actual change.
                        if (it.layoutParams.height != heightLayoutParam) {
                            it.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, heightLayoutParam)
                        }
                    },
                )
            }
        }

        view?.let { wv ->
            // Capture in composition so the effect keys and body see the same values.
            val content = controller.content
            val generation = controller.contentGeneration
            LaunchedEffect(wv, content, generation) {
                // Skip loads superseded by a native-state restore; explicit load calls bump the generation and still win.
                if (generation == controller.restoredGeneration) return@LaunchedEffect
                when (content) {
                    is WebContent.Url -> wv.loadUrl(content.url, content.headers)
                    is WebContent.Html -> wv.loadDataWithBaseURL(content.baseUrl, content.html, "text/html", "utf-8", null)
                    null -> Unit
                }
            }
            LaunchedEffect(wv) {
                controller.commands.collect { cmd ->
                    when (cmd) {
                        WebViewCommand.GoBack -> if (wv.canGoBack()) wv.goBack()
                        WebViewCommand.GoForward -> if (wv.canGoForward()) wv.goForward()
                        WebViewCommand.Reload -> wv.reload()
                        WebViewCommand.StopLoading -> {
                            wv.stopLoading()
                            // stopLoading does not reliably deliver onPageFinished.
                            if (controller.loadingState is LoadingState.Loading) {
                                controller.loadingState = LoadingState.Finished
                            }
                        }
                    }
                }
            }
        }
    }
}

// Chromium's WebView shell refuses to restore states above this size; the Binder transaction buffer is ~1 MB process-wide.
private const val MAX_SAVED_STATE_BYTES = 300 * 1024

/** Serialized navigation history, or null when saving fails or the result is too large to put in a saved-instance-state Bundle. */
private fun WebView.captureState(): Bundle? {
    val bundle = Bundle()
    if (saveState(bundle) == null) return null
    val parcel = Parcel.obtain()
    try {
        parcel.writeBundle(bundle)
        if (parcel.dataSize() > MAX_SAVED_STATE_BYTES) return null
    } finally {
        parcel.recycle()
    }
    return bundle
}

/** Memoizes one [captureState] result per navigation state; both saveState and the Parcel round-trip are costly. */
private class StateCapture {

    private var captured: Bundle? = null
    private var isCaptured = false

    /** Drops the memoized result. Called from every client callback that can change the back/forward list. */
    fun invalidate() {
        captured = null
        isCaptured = false
    }

    fun capture(view: WebView): Bundle? {
        if (!isCaptured) {
            captured = view.captureState()
            isCaptured = true
        }
        return captured
    }
}

private fun WebView.applySettings(s: WebViewSettings) = with(settings) {
    javaScriptEnabled = s.javaScriptEnabled
    domStorageEnabled = s.domStorageEnabled
    javaScriptCanOpenWindowsAutomatically = s.javaScriptCanOpenWindowsAutomatically
    mediaPlaybackRequiresUserGesture = s.mediaPlaybackRequiresUserGesture
    setSupportZoom(s.zoomEnabled)
    builtInZoomControls = s.zoomEnabled
    displayZoomControls = false
    s.userAgent?.let { userAgentString = it }
    s.backgroundColor?.let { this@applySettings.setBackgroundColor(it.toArgb()) }
}

private class ControllerClient(
    private val c: WebViewController,
    private val stateCapture: StateCapture,
    private val onProcessGone: () -> Unit,
    var uriHandler: UriHandler,
) : WebViewClient() {

    /** URL of the navigation the last onPageStarted announced; cleared once its onPageFinished has been handled. */
    var inFlightUrl: String? = null
        private set

    /**
     * True after a main frame navigation was cancelled here. Chromium still reports a start and a completion for it,
     * which must not surface as a loading state.
     */
    var navigationCancelled: Boolean = false

    /** Main frame HTTP error for the navigation that is about to start; Chromium delivers it before onPageStarted. */
    private var pendingHttpError: WebViewHttpError? = null

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val cancelled = cancelNavigation(request)
        if (request.isForMainFrame) navigationCancelled = cancelled
        return cancelled
    }

    private fun cancelNavigation(request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        val interceptor = c.navigationInterceptor
        if (interceptor != null) {
            val nav = NavigationRequest(
                url = url,
                isMainFrame = request.isForMainFrame,
                type = if (request.hasGesture()) NavigationType.LinkActivated else NavigationType.Other,
                isRedirect = request.isRedirect,
            )
            if (interceptor.onNavigation(nav) == NavigationDecision.Cancel) return true
        }
        if (!c.settings.shouldOpenExternally(url)) return false
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            // AndroidUriHandler throws IllegalArgumentException when no activity can open the URI, and
            // startActivity from a non-Activity context raises AndroidRuntimeException. Letting either escape
            // into WebView's callback would kill the process; the navigation stays cancelled either way,
            // which beats the ERR_UNKNOWN_URL_SCHEME error page.
            Log.w("KaitekiWebView", "no app could open $url", e)
        }
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        stateCapture.invalidate()
        inFlightUrl = url
        navigationCancelled = false
        c.lastError = null
        // The error arrives before this callback; only one belonging to the load being started survives.
        c.lastHttpError = pendingHttpError?.takeIf { it.url == url }
        pendingHttpError = null
        c.title = null
        // Progress reported before the commit belongs to this navigation; keep it instead of jumping back to zero.
        if (c.loadingState !is LoadingState.Loading) c.loadingState = LoadingState.Loading(0f)
        c.currentUrl = url
    }

    override fun onPageFinished(view: WebView, url: String?) {
        // Chromium reports a load aborted by a newer navigation as onPageFinished for the abandoned URL. onPageStarted
        // fires on commit, so an aborted load never became the in-flight one.
        if (url != inFlightUrl) return
        stateCapture.invalidate()
        inFlightUrl = null
        c.loadingState = LoadingState.Finished
        c.crashAutoReloadAttempted = false
        c.currentUrl = url
        c.canGoBack = view.canGoBack()
        c.canGoForward = view.canGoForward()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        // Also fires for same-document navigations (pushState, hash), which skip onPageStarted/Finished.
        stateCapture.invalidate()
        if (url != null) c.currentUrl = url
        c.canGoBack = view.canGoBack()
        c.canGoForward = view.canGoForward()
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        // Also fires for every failing subresource of an otherwise healthy page.
        if (!request.isForMainFrame) return
        pendingHttpError = WebViewHttpError(errorResponse.statusCode, request.url.toString())
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (!request.isForMainFrame) return
        c.lastError = WebViewError(
            code = error.errorCode.toLong(),
            description = error.description.toString(),
            failingUrl = request.url.toString(),
        )
        c.loadingState = LoadingState.Finished
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        // Returning false would kill the whole app process; report the crash and recreate the native view instead.
        c.lastError = WebViewError(
            code = ERROR_UNKNOWN.toLong(),
            description = "web content process terminated",
            failingUrl = c.currentUrl,
        )
        if (c.loadingState is LoadingState.Loading) c.loadingState = LoadingState.Finished
        onProcessGone()
        return true
    }
}

private class ControllerChromeClient(private val c: WebViewController, private val client: ControllerClient) : WebChromeClient() {

    private val dialogScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun dispose() {
        dialogScope.cancel()
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        if (newProgress < 100) {
            // The first estimate arrives when a navigation starts, before the commit that delivers onPageStarted;
            // it is the only signal covering the network wait.
            if (client.navigationCancelled && c.loadingState !is LoadingState.Loading) return
            c.loadingState = LoadingState.Loading(newProgress / 100f)
        } else {
            client.navigationCancelled = false
            // Complete without a committed page: the navigation was cancelled, failed or became a download,
            // so no onPageFinished will follow.
            if (c.loadingState is LoadingState.Loading && client.inFlightUrl == null) c.loadingState = LoadingState.Finished
        }
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        c.title = title
    }

    // Returning false shows the framework's default dialog. Page JS stays blocked until the JsResult
    // completes, so runDialog must deliver in every case, including a scope already cancelled at launch.

    override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        val handler = c.jsDialogHandler ?: return false
        return runDialog(complete = { if (it != null) result.confirm() else result.cancel() }) {
            handler.onAlert(JsDialog.Alert(message.orEmpty(), url.orEmpty()))
        }
    }

    override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        val handler = c.jsDialogHandler ?: return false
        return runDialog(complete = { if (it == true) result.confirm() else result.cancel() }) {
            handler.onConfirm(JsDialog.Confirm(message.orEmpty(), url.orEmpty()))
        }
    }

    override fun onJsPrompt(view: WebView, url: String?, message: String?, defaultValue: String?, result: JsPromptResult): Boolean {
        val handler = c.jsDialogHandler ?: return false
        return runDialog(complete = { if (it != null) result.confirm(it) else result.cancel() }) {
            handler.onPrompt(JsDialog.Prompt(message.orEmpty(), defaultValue.orEmpty(), url.orEmpty()))
        }
    }

    /**
     * Runs [block] and hands its outcome to [complete] exactly once, or null when the handler was
     * cancelled or threw. invokeOnCompletion also fires when the scope is already cancelled and the
     * coroutine body never runs. JsResult completion is safe from any thread and after destroy().
     */
    private fun <T> runDialog(complete: (T?) -> Unit, block: suspend () -> T): Boolean {
        var outcome: T? = null
        dialogScope.launch {
            outcome = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // An uncaught exception would kill the process before the JsResult completes.
                Log.w("KaitekiWebView", "jsDialogHandler threw; completing dialog as cancelled", e)
                null
            }
        }.invokeOnCompletion { complete(outcome) }
        return true
    }
}
