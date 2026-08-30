package com.kroegerama.kmp.kaiteki.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSLog
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLErrorBadURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorFailingURLStringErrorKey
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.Foundation.setValue
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIColor
import platform.UIKit.UITextField
import platform.UIKit.UIViewController
import platform.WebKit.WKAudiovisualMediaTypeAll
import platform.WebKit.WKAudiovisualMediaTypeNone
import platform.WebKit.WKErrorWebContentProcessTerminated
import platform.WebKit.WKFrameInfo
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationTypeBackForward
import platform.WebKit.WKNavigationTypeFormResubmitted
import platform.WebKit.WKNavigationTypeFormSubmitted
import platform.WebKit.WKNavigationTypeLinkActivated
import platform.WebKit.WKNavigationTypeOther
import platform.WebKit.WKNavigationTypeReload
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.objc.class_addMethod
import platform.objc.object_getClass
import platform.objc.sel_registerName
import kotlin.coroutines.resume

public actual typealias WebViewSavedState = NSData

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformWebView(
    controller: WebViewController,
    modifier: Modifier,
    handleBackNavigation: Boolean,
) {
    // Delegates, settings and command collection bind to one controller instance; recreate the native view when it changes.
    key(controller) {
        // Must be remembered: navigationDelegate/UIDelegate are weak references, and addObserver does not retain the observer.
        val delegate = remember { NavDelegate(controller) }
        val hostViewController = LocalUIViewController.current
        val uiDelegate = remember { UiDelegate(controller, delegate, hostViewController) }
        SideEffect { uiDelegate.hostViewController = hostViewController }
        val observer = remember { StateObserver(controller) }
        var view by remember { mutableStateOf<WKWebView?>(null) }

        UIKitView(
            modifier = modifier,
            factory = {
                WKWebView(
                    frame = CGRectZero.readValue(),
                    configuration = buildConfiguration(controller.settings),
                ).apply {
                    navigationDelegate = delegate
                    UIDelegate = uiDelegate
                    observer.attach(this)
                    controller.settings.userAgent?.let { customUserAgent = it }
                    if (!controller.settings.zoomEnabled) {
                        scrollView.pinchGestureRecognizer?.enabled = false
                    }
                    scrollView.scrollEnabled = controller.settings.scrollEnabled
                    applyBackground(controller.settings.backgroundColor)
                    // cinterop drops WK_API_AVAILABLE, so this compiles against deployment targets below
                    // iOS 16.4, where the selector is missing and the call would raise doesNotRecognizeSelector.
                    if (controller.settings.inspectable && respondsToSelector(sel_registerName("setInspectable:"))) {
                        inspectable = true
                    }
                    // A skip marker from a previous view must never suppress this view's load.
                    controller.restoredGeneration = -1
                    controller.pendingRestore?.let { pending ->
                        controller.pendingRestore = null
                        // Restores back/forward list, current page and scroll position. Success is not observable:
                        // WebKit silently ignores an incompatible blob, leaving a blank view until a manual load.
                        interactionState = pending.state
                        // Skip only the stash-time generation's load; loads issued after the stash bump the
                        // generation past it and still run, on top of the restored history.
                        controller.restoredGeneration = pending.generation
                    }
                    controller.nativeStateProvider = { interactionState as? NSData }
                    view = this
                }
            },
            update = { wv ->
                // The native edge-swipe through web history is iOS's back handling.
                wv.allowsBackForwardNavigationGestures = handleBackNavigation
            },
            onRelease = { wv ->
                // Cancels pending dialog handlers; their WebKit completion handlers are called (as cancelled),
                // which is mandatory: an uncalled completion handler raises NSInternalInconsistencyException.
                uiDelegate.dispose()
                controller.nativeStateProvider = null
                // Stash state so a recreated view under the same controller restores history instead of skipping its load.
                controller.stashState(wv.interactionState as? NSData)
                view = null
                wv.stopLoading()
                observer.detach()
                wv.navigationDelegate = null
                wv.UIDelegate = null
            },
            properties = UIKitInteropProperties(
                // A scrolling web view must win the touch: the cooperative delay makes its own scrolling
                // lag by that delay. Without scrolling of its own, cooperative lets Compose parents pan instead.
                interactionMode = if (controller.settings.scrollEnabled) {
                    UIKitInteropInteractionMode.NonCooperative
                } else {
                    UIKitInteropInteractionMode.Cooperative()
                },
                // Compose semantics cannot describe a rendered page; without this the web view is invisible to VoiceOver.
                isNativeAccessibilityEnabled = controller.settings.nativeAccessibilityEnabled,
            ),
        )

        view?.let { wv ->
            // Capture in composition so the effect keys and body see the same values.
            val content = controller.content
            val generation = controller.contentGeneration
            LaunchedEffect(wv, content, generation) {
                // A native-state restore replaced this generation's load; explicit load calls bump the generation.
                if (generation == controller.restoredGeneration) return@LaunchedEffect
                when (content) {
                    is WebContent.Url -> {
                        val u = NSURL.URLWithString(content.url)
                        if (u == null) {
                            controller.lastError = WebViewError(
                                code = NSURLErrorBadURL,
                                description = "Invalid URL",
                                failingUrl = content.url,
                            )
                        } else {
                            u.absoluteString?.let { delegate.markProgrammatic(it) }
                            val req = NSMutableURLRequest(uRL = u)
                            content.headers.forEach { (k, v) -> req.setValue(v, forHTTPHeaderField = k) }
                            wv.loadRequest(req)
                        }
                    }

                    is WebContent.Html -> {
                        val base = content.baseUrl?.let { NSURL.URLWithString(it) }
                        delegate.markProgrammatic(base?.absoluteString ?: "about:blank")
                        wv.loadHTMLString(string = content.html, baseURL = base)
                    }

                    null -> Unit
                }
            }
            LaunchedEffect(wv) {
                controller.commands.collect { cmd ->
                    when (cmd) {
                        WebViewCommand.GoBack -> wv.goBack()
                        WebViewCommand.GoForward -> wv.goForward()
                        WebViewCommand.Reload -> wv.reload()
                        WebViewCommand.StopLoading -> {
                            wv.stopLoading()
                            // A stop after the navigation committed delivers neither didFinish nor didFail.
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

private fun WKWebView.applyBackground(color: Color?) {
    if (color == null) return
    val uiColor = color.toUIColor()
    if (color.alpha < 1f) {
        // Turning off opaque is what propagates transparency to every frame view in the web process;
        // the overscroll area then ignores underPageBackgroundColor.
        opaque = false
        backgroundColor = uiColor
    } else {
        // backgroundColor covers the gap before the first paint, the override keeps the color afterwards:
        // once the page supplies a background of its own it otherwise wins.
        backgroundColor = uiColor
        underPageBackgroundColor = uiColor
    }
    // scrollView.backgroundColor must stay untouched: WKScrollView latches the first client assignment
    // and from then on ignores both underPageBackgroundColor and the non-opaque path.
}

private fun Color.toUIColor(): UIColor {
    // Components are relative to the color's own color space, while UIColor expects sRGB.
    val srgb = convert(ColorSpaces.Srgb)
    return UIColor(
        red = srgb.red.toDouble(),
        green = srgb.green.toDouble(),
        blue = srgb.blue.toDouble(),
        alpha = srgb.alpha.toDouble(),
    )
}

private fun buildConfiguration(s: WebViewSettings) = WKWebViewConfiguration().apply {
    defaultWebpagePreferences.allowsContentJavaScript = s.javaScriptEnabled
    preferences.javaScriptCanOpenWindowsAutomatically = s.javaScriptCanOpenWindowsAutomatically
    allowsInlineMediaPlayback = true
    mediaTypesRequiringUserActionForPlayback = if (s.mediaPlaybackRequiresUserGesture) WKAudiovisualMediaTypeAll else WKAudiovisualMediaTypeNone
    if (!s.zoomEnabled) {
        // WKWebView re-arms its zoom gesture recognizers after each navigation; enforcing the viewport is the only reliable way to disable zoom.
        userContentController.addUserScript(
            WKUserScript(
                source = "var meta=document.createElement('meta');" +
                        "meta.name='viewport';" +
                        "meta.content='width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no';" +
                        "document.head.appendChild(meta);",
                injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
                forMainFrameOnly = true,
            )
        )
    }
    // domStorageEnabled has no WKWebView equivalent; it is always on.
}

/**
 * Copies the observable navigation state onto [c]. [refreshProgress] also takes the current load progress,
 * which is stale outside an `estimatedProgress` notification.
 */
private fun WKWebView.syncTo(c: WebViewController, refreshProgress: Boolean) {
    c.currentUrl = URL?.absoluteString
    // WKWebView reports an untitled page as an empty string, where Android reports null.
    c.title = title?.takeIf { it.isNotEmpty() }
    c.canGoBack = canGoBack
    c.canGoForward = canGoForward
    if (!c.settings.zoomEnabled) {
        // WKWebView re-arms its zoom gesture recognizers after each navigation.
        scrollView.pinchGestureRecognizer?.enabled = false
    }
    if (refreshProgress && loading) c.loadingState = LoadingState.Loading(estimatedProgress.toFloat())
}

// WKWebView documents these properties as KVO-compliant; delegate callbacks alone miss same-document navigations (pushState, hash).
private val observedKeyPaths = listOf("URL", "title", "canGoBack", "canGoForward", "estimatedProgress")

@OptIn(ExperimentalForeignApi::class)
private class StateObserver(private val c: WebViewController) : NSObject() {

    private var webView: WKWebView? = null

    fun attach(wv: WKWebView) {
        installKvoSelectorOnce(this)
        webView = wv
        observedKeyPaths.forEach { wv.addObserver(this, forKeyPath = it, options = NSKeyValueObservingOptionNew, context = null) }
    }

    fun detach() {
        val wv = webView ?: return
        webView = null
        observedKeyPaths.forEach { wv.removeObserver(this, forKeyPath = it) }
    }

    /** Snapshot sync on every KVO notification; [progressChanged] marks an `estimatedProgress` notification. */
    fun sync(progressChanged: Boolean) {
        webView?.syncTo(c, refreshProgress = progressChanged)
    }
}

private var kvoSelectorInstalled = false

/**
 * Kotlin/Native cannot override `observeValueForKeyPath:ofObject:change:context:` (cinterop exposes it as a
 * final extension function), so the selector is installed on the generated class via the Objective-C runtime.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun installKvoSelectorOnce(instance: NSObject) {
    if (kvoSelectorInstalled) return
    kvoSelectorInstalled = true

    // self, _cmd, keyPath, object, change, context: only self and keyPath are decoded; the owner re-reads the observed properties.
    val implementation = staticCFunction<
            COpaquePointer?,
            COpaquePointer?,
            COpaquePointer?,
            COpaquePointer?,
            COpaquePointer?,
            COpaquePointer?,
            Unit,
            > { self, _, keyPath, _, _, _ ->
        val progressChanged = keyPath?.let { interpretObjCPointerOrNull<NSString>(it.rawValue) }?.toString() == "estimatedProgress"
        self?.let { interpretObjCPointerOrNull<StateObserver>(it.rawValue) }?.sync(progressChanged)
    }

    val added = class_addMethod(
        object_getClass(instance),
        sel_registerName("observeValueForKeyPath:ofObject:change:context:"),
        implementation.reinterpret(),
        "v@:@@@^v",
    )
    check(added) { "could not install the KVO selector on the generated Objective-C class" }
}

/**
 * Canonical form of a URL string: lowercased scheme and host, default port dropped, empty path written as `/`.
 * Loads are keyed on this so a caller-supplied URL matches the normalized one WebKit reports back.
 */
private fun normalizeUrlKey(url: String): String {
    val components = NSURLComponents.componentsWithString(url) ?: return url
    components.scheme = components.scheme?.lowercase()
    components.host = components.host?.lowercase()
    val port = components.port?.intValue
    if (port != null && port == defaultPortForScheme(components.scheme)) components.port = null
    if (!components.host.isNullOrEmpty() && components.path.isNullOrEmpty()) components.path = "/"
    return components.string ?: url
}

private fun defaultPortForScheme(scheme: String?): Int? = when (scheme) {
    "http", "ws" -> 80
    "https", "wss" -> 443
    "ftp" -> 21
    else -> null
}

private fun WKNavigationAction.toNavigationType(): NavigationType = when (navigationType) {
    WKNavigationTypeLinkActivated -> NavigationType.LinkActivated
    WKNavigationTypeFormSubmitted,
    WKNavigationTypeFormResubmitted -> NavigationType.FormSubmitted

    else -> NavigationType.Other
}

@OptIn(BetaInteropApi::class)
private class NavDelegate(private val c: WebViewController) : NSObject(), WKNavigationDelegateProtocol {

    /**
     * URLs of loads triggered by this wrapper (controller.content, re-issued `target="_blank"` requests),
     * exempt from interception. Keyed per URL: overlapping loads must not evict each other. Entries are
     * consumed by their policy pass or navigation start; the value counts how many commits a leftover
     * entry may outlive before `didCommitNavigation` ages it out.
     */
    private val pendingProgrammaticUrls = mutableMapOf<String, Int>()

    fun markProgrammatic(url: String) {
        pendingProgrammaticUrls[normalizeUrlKey(url)] = 1
    }

    private fun consumeProgrammatic(url: String) = pendingProgrammaticUrls.remove(normalizeUrlKey(url)) != null

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val action = decidePolicyForNavigationAction
        val url = action.request.URL?.absoluteString

        val allowed = when {
            // Programmatic loads always carry WKNavigationTypeOther; the type check keeps a stale entry
            // from exempting a link click or form submit to the same URL.
            url != null && action.navigationType == WKNavigationTypeOther && consumeProgrammatic(url) -> true

            // Android parity: WebView does not intercept reload or history navigations.
            action.navigationType == WKNavigationTypeReload || action.navigationType == WKNavigationTypeBackForward -> true

            else -> {
                val interceptor = c.navigationInterceptor
                if (interceptor == null || url == null) true
                else interceptor.onNavigation(
                    NavigationRequest(
                        url = url,
                        isMainFrame = action.targetFrame?.mainFrame ?: true,
                        type = action.toNavigationType(),
                    )
                ) == NavigationDecision.Allow
            }
        }

        if (allowed && action.targetFrame == null) {
            // Load target="_blank" in this view (Android parity); mark it as ours so its policy pass skips the
            // interceptor. Cancelling also keeps WebKit from requesting a new web view via the UI delegate.
            url?.let { markProgrammatic(it) }
            webView.loadRequest(action.request)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            return
        }
        decisionHandler(
            if (allowed) WKNavigationActionPolicy.WKNavigationActionPolicyAllow
            else WKNavigationActionPolicy.WKNavigationActionPolicyCancel
        )
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        // A started load is past its policy pass; drop its exemption so it cannot linger
        // (loadHTMLString may skip the policy pass entirely).
        webView.URL?.absoluteString?.let { consumeProgrammatic(it) }
        c.lastError = null
        // The previous page's title must not linger into the new load.
        c.title = null
        // KVO may deliver an early progress estimate before this callback; keep it.
        if (c.loadingState !is LoadingState.Loading) {
            c.loadingState = LoadingState.Loading(null)
        }
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
        // Age out entries whose policy callback never fired (loadHTMLString bypass, superseded loads) so they cannot
        // exempt a later navigation. Entries survive one commit: the policy pass for a fresh programmatic load may run
        // after an unrelated in-flight navigation commits, so a just-added entry must not be dropped by that commit.
        val iterator = pendingProgrammaticUrls.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value <= 0) iterator.remove() else entry.setValue(entry.value - 1)
        }
        webView.syncTo(c, refreshProgress = true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        // loading is true when this callback belongs to a superseded navigation while a newer one is in flight.
        if (!webView.loading) {
            c.loadingState = LoadingState.Finished
            c.crashAutoReloadAttempted = false
        }
        webView.syncTo(c, refreshProgress = true)
    }

    override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
        // The content process died (typically memory pressure); the view goes blank and no didFail arrives.
        c.lastError = WebViewError(
            code = WKErrorWebContentProcessTerminated,
            description = "web content process terminated",
            failingUrl = webView.URL?.absoluteString ?: c.currentUrl,
        )
        if (c.loadingState is LoadingState.Loading) c.loadingState = LoadingState.Finished
        // Recover once per finished load: enough for the common memory-pressure kill, without looping on a page that dies on every load.
        if (!c.crashAutoReloadAttempted) {
            c.crashAutoReloadAttempted = true
            // Re-driving the content also recovers inline HTML, which has no back/forward item for reload() to restore.
            c.fallbackToCurrentUrl()
            c.redriveContent()
        }
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) = fail(webView, withError)

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) = fail(webView, withError)

    private fun fail(webView: WKWebView, error: NSError) {
        // Superseded loads (stopLoading, a newer load, interceptor cancel) are not errors, but a lingering Loading state must still end.
        // loading is true when the failure belongs to a superseded navigation while a newer one is in flight; leave that load alone.
        val cancelled = (error.domain == NSURLErrorDomain && error.code == NSURLErrorCancelled) ||
                (error.domain == "WebKitErrorDomain" && error.code == 102L)
        if (cancelled) {
            if (!webView.loading && c.loadingState is LoadingState.Loading) c.loadingState = LoadingState.Finished
            return
        }
        if (!webView.loading) {
            c.lastError = WebViewError(
                code = error.code,
                description = error.localizedDescription,
                // On provisional failures webView.URL still points at the previous committed page.
                failingUrl = error.userInfo[NSURLErrorFailingURLStringErrorKey] as? String ?: webView.URL?.absoluteString,
            )
            c.loadingState = LoadingState.Finished
        }
    }
}

/**
 * Handles JS dialogs and `window.open`. Every dialog's WebKit completion handler must be called exactly
 * once on the main thread; an uncalled or double-called handler raises NSInternalInconsistencyException.
 * All entry points run on the main thread and [dialogScope] uses the main dispatcher, so that holds
 * throughout; WebKit additionally serializes dialogs per web view, so at most one is pending at a time.
 */
private class UiDelegate(
    private val c: WebViewController,
    private val navDelegate: NavDelegate,
    var hostViewController: UIViewController,
) : NSObject(), WKUIDelegateProtocol {

    private val defaultHandler = UiKitJsDialogHandler { hostViewController }
    private val dialogScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun dispose() {
        dialogScope.cancel()
    }

    override fun webView(
        webView: WKWebView,
        createWebViewWithConfiguration: WKWebViewConfiguration,
        forNavigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures,
    ): WKWebView? {
        // Only window.open lands here: target="_blank" clicks are cancelled and re-issued by the navigation
        // delegate's policy pass before WebKit would request a new web view. Returning null opens no window;
        // Android parity is loading the request in this view after consulting the interceptor (the re-issued
        // load is marked programmatic, so its own policy pass skips the interceptor).
        val action = forNavigationAction
        if (action.targetFrame != null) return null
        val url = action.request.URL?.absoluteString
        // window.open() without a URL produces an empty request; loading it would blank the current page.
        if (url.isNullOrEmpty() || url == "about:blank") return null
        val interceptor = c.navigationInterceptor
        val allowed = interceptor == null || interceptor.onNavigation(
            NavigationRequest(url = url, isMainFrame = true, type = action.toNavigationType())
        ) == NavigationDecision.Allow
        if (allowed) {
            navDelegate.markProgrammatic(url)
            webView.loadRequest(action.request)
        }
        return null
    }

    override fun webView(
        webView: WKWebView,
        runJavaScriptAlertPanelWithMessage: String,
        initiatedByFrame: WKFrameInfo,
        completionHandler: () -> Unit,
    ) {
        val dialog = JsDialog.Alert(runJavaScriptAlertPanelWithMessage, initiatedByFrame.originString())
        runDialog(complete = { completionHandler() }) { handler().onAlert(dialog) }
    }

    override fun webView(
        webView: WKWebView,
        runJavaScriptConfirmPanelWithMessage: String,
        initiatedByFrame: WKFrameInfo,
        completionHandler: (Boolean) -> Unit,
    ) {
        val dialog = JsDialog.Confirm(runJavaScriptConfirmPanelWithMessage, initiatedByFrame.originString())
        runDialog(complete = { completionHandler(it == true) }) { handler().onConfirm(dialog) }
    }

    override fun webView(
        webView: WKWebView,
        runJavaScriptTextInputPanelWithPrompt: String,
        defaultText: String?,
        initiatedByFrame: WKFrameInfo,
        completionHandler: (String?) -> Unit,
    ) {
        val dialog = JsDialog.Prompt(runJavaScriptTextInputPanelWithPrompt, defaultText.orEmpty(), initiatedByFrame.originString())
        runDialog(complete = { completionHandler(it) }) { handler().onPrompt(dialog) }
    }

    private fun handler(): JsDialogHandler = c.jsDialogHandler ?: defaultHandler

    /**
     * Runs [block] and hands its outcome to [complete] exactly once, or null when the handler was cancelled or
     * threw. invokeOnCompletion also fires when the scope is already cancelled and the coroutine body never
     * runs; completion then happens synchronously on the cancelling (main) thread.
     */
    private fun <T> runDialog(complete: (T?) -> Unit, block: suspend () -> T) {
        var outcome: T? = null
        dialogScope.launch {
            outcome = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // An uncaught exception would terminate the app before the WebKit completion handler runs.
                NSLog("KaitekiWebView: jsDialogHandler threw; completing dialog as cancelled: %@", e.stackTraceToString())
                null
            }
        }.invokeOnCompletion { complete(outcome) }
    }
}

private fun WKFrameInfo.originString(): String {
    val origin = securityOrigin
    val host = origin.host
    // Opaque origins (about:blank, data: URLs) have an empty host.
    if (host.isEmpty()) return ""
    val port = origin.port
    return if (port != 0L) "${origin.protocol}://$host:$port" else "${origin.protocol}://$host"
}

/** Default dialogs: a UIAlertController titled with the requesting origin, presented over the hosting view controller. */
private class UiKitJsDialogHandler(private val host: () -> UIViewController) : JsDialogHandler {

    override suspend fun onAlert(dialog: JsDialog.Alert): Unit = suspendCancellableCoroutine { cont ->
        val alert = makeAlert(dialog)
        alert.addAction(UIAlertAction.actionWithTitle(uiKitLocalized("OK"), UIAlertActionStyleDefault) { cont.resumeIfActive(Unit) })
        present(alert, cont, cancelled = Unit)
    }

    override suspend fun onConfirm(dialog: JsDialog.Confirm): Boolean = suspendCancellableCoroutine { cont ->
        val alert = makeAlert(dialog)
        alert.addAction(UIAlertAction.actionWithTitle(uiKitLocalized("Cancel"), UIAlertActionStyleCancel) { cont.resumeIfActive(false) })
        alert.addAction(UIAlertAction.actionWithTitle(uiKitLocalized("OK"), UIAlertActionStyleDefault) { cont.resumeIfActive(true) })
        present(alert, cont, cancelled = false)
    }

    override suspend fun onPrompt(dialog: JsDialog.Prompt): String? = suspendCancellableCoroutine { cont ->
        val alert = makeAlert(dialog)
        alert.addTextFieldWithConfigurationHandler { it?.text = dialog.defaultValue }
        alert.addAction(UIAlertAction.actionWithTitle(uiKitLocalized("Cancel"), UIAlertActionStyleCancel) { cont.resumeIfActive(null) })
        alert.addAction(UIAlertAction.actionWithTitle(uiKitLocalized("OK"), UIAlertActionStyleDefault) {
            cont.resumeIfActive((alert.textFields?.firstOrNull() as? UITextField)?.text.orEmpty())
        })
        present(alert, cont, cancelled = null)
    }

    private fun makeAlert(dialog: JsDialog) = UIAlertController.alertControllerWithTitle(
        title = dialog.sourceUrl.ifEmpty { null },
        message = dialog.message,
        preferredStyle = UIAlertControllerStyleAlert,
    )

    private fun <T> present(alert: UIAlertController, cont: CancellableContinuation<T>, cancelled: T) {
        cont.invokeOnCancellation {
            // May be invoked from any thread; UIKit requires main. An action tap resumes first and dismisses
            // the alert itself, so this only runs while it is still up (or was never presented, when it is a no-op).
            dispatch_async(dispatch_get_main_queue()) { alert.dismissViewControllerAnimated(false, null) }
        }
        presenter().presentViewController(alert, animated = true, completion = null)
        // UIKit refuses some presentations with only a logged warning (presenter mid-transition, view detached).
        // Detect that on the next runloop tick and complete as cancelled: dialogs are serialized per web view,
        // so an unresumed continuation would otherwise block the page's JS until the WebView leaves composition.
        dispatch_async(dispatch_get_main_queue()) {
            if (alert.presentingViewController == null && !alert.isBeingPresented()) cont.resumeIfActive(cancelled)
        }
    }

    /** Topmost view controller that can present, skipping one that is being dismissed. */
    private fun presenter(): UIViewController {
        var vc = host()
        while (true) {
            val presented = vc.presentedViewController
            if (presented == null || presented.isBeingDismissed()) return vc
            vc = presented
        }
    }
}

/** Resume at most once; action taps and cancellation both run on the main thread, so this cannot race. */
private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) resume(value)
}

/** UIKit's own localized button titles, falling back to the English key when unavailable. */
private fun uiKitLocalized(key: String): String =
    NSBundle.bundleWithIdentifier("com.apple.UIKit")?.localizedStringForKey(key, key, null) ?: key
