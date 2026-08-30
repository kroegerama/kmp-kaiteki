package com.kroegerama.kmp.kaiteki.webview

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public sealed interface WebContent {
    public data class Url(val url: String, val headers: Map<String, String> = emptyMap()) : WebContent
    public data class Html(val html: String, val baseUrl: String? = null) : WebContent
}

@Immutable
public sealed interface LoadingState {
    public data object Idle : LoadingState

    /** [progress] is null on iOS until the first estimate arrives. */
    public data class Loading(val progress: Float?) : LoadingState
    public data object Finished : LoadingState
}

@Immutable
public data class WebViewError(
    public val code: Long,
    public val description: String,
    public val failingUrl: String?,
)

/**
 * A main frame response with an HTTP status of 400 or above. Reported separately from [WebViewError]:
 * the load itself succeeded and the page renders whatever body the server sent with the status.
 */
@Immutable
public data class WebViewHttpError(
    public val statusCode: Int,
    public val url: String,
)

/**
 * Opaque, platform-serialized web view state: navigation history, plus scroll position on iOS.
 * Android: `android.os.Bundle` from `WebView.saveState`; iOS: `NSData` from `WKWebView.interactionState`.
 */
public expect class WebViewSavedState

/** Native saved state tagged with the [WebViewController.contentGeneration] it was captured at. */
internal class PendingRestore(val state: WebViewSavedState, val generation: Int)

internal sealed interface WebViewCommand {
    data object GoBack : WebViewCommand
    data object GoForward : WebViewCommand
    data object Reload : WebViewCommand
    data object StopLoading : WebViewCommand
}

/** Drives a single [WebView]; attaching it to more than one WebView at a time is unsupported. */
@Stable
public class WebViewController(
    initialUrl: String? = null,
    public val settings: WebViewSettings = WebViewSettings(),
) {
    /** Desired content. Snapshot state, so it survives native view recreation. */
    internal var content: WebContent? by mutableStateOf(initialUrl?.let { WebContent.Url(it) })
        private set

    /** Bumped on every load call so re-requesting equal content still triggers a load. */
    internal var contentGeneration: Int by mutableIntStateOf(0)
        private set

    public var currentUrl: String? by mutableStateOf(initialUrl)
        internal set
    public var title: String? by mutableStateOf(null)
        internal set
    public var loadingState: LoadingState by mutableStateOf(LoadingState.Idle)
        internal set
    public var canGoBack: Boolean by mutableStateOf(false)
        internal set
    public var canGoForward: Boolean by mutableStateOf(false)
        internal set
    public var lastError: WebViewError? by mutableStateOf(null)
        internal set

    /** Status of the current page when the server answered with 400 or above. Replaced when the next page commits. */
    public var lastHttpError: WebViewHttpError? by mutableStateOf(null)
        internal set

    /**
     * Set anytime, including before a view is attached. Null = allow everything.
     * Not snapshot state: read only from platform callbacks, never during composition.
     */
    public var navigationInterceptor: NavigationInterceptor? = null

    /**
     * Set anytime, including before a view is attached. Null = platform default dialogs:
     * Android shows the framework dialog (requires an Activity context; elsewhere dialogs are silently
     * cancelled), iOS presents a `UIAlertController`.
     * Not snapshot state: read only from platform callbacks, never during composition.
     */
    public var jsDialogHandler: JsDialogHandler? = null

    /** Captures state from the attached native view. Set while a view is attached; not snapshot state. */
    internal var nativeStateProvider: (() -> WebViewSavedState?)? = null

    /** Consumed by the next native view on creation; also the in-memory stash across plain view recreation. */
    internal var pendingRestore: PendingRestore? = null

    /** Content generation whose load is skipped because native state was restored instead. */
    internal var restoredGeneration: Int = -1

    /** True after an automatic reload following a web-content-process crash; cleared when a page finishes loading or new content is requested. */
    internal var crashAutoReloadAttempted: Boolean = false

    /** Content generation whose automatic reload is suppressed after repeated web-content-process crashes. */
    internal var crashSuppressedGeneration: Int = -1

    /** Stashes captured native state tagged with the current generation; falls back to the current URL when capture failed. */
    internal fun stashState(state: WebViewSavedState?) {
        pendingRestore = state?.let { PendingRestore(it, contentGeneration) }
        if (state == null) fallbackToCurrentUrl()
    }

    /**
     * Retargets the pending content to the last visited URL, so a view recreated without restorable
     * native state reloads the page the user was on instead of the originally requested content.
     * Inline HTML content is kept as is.
     */
    internal fun fallbackToCurrentUrl() {
        val url = currentUrl ?: return
        // Keep the original content (and its headers) when the user never navigated away from it.
        if ((content as? WebContent.Url)?.url == url) return
        // Inline HTML is held by the controller and can be re-driven as is; a URL load would lose it.
        if (content is WebContent.Html) return
        content = WebContent.Url(url)
        contentGeneration++
    }

    /** Re-runs the load for the current content without changing it. */
    internal fun redriveContent() {
        contentGeneration++
    }

    // replay = 0: one-shot commands issued with no WebView attached (or in the first frames
    // after creation, before collection starts) are dropped.
    private val _commands = MutableSharedFlow<WebViewCommand>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    internal val commands: SharedFlow<WebViewCommand> = _commands.asSharedFlow()

    public fun loadUrl(url: String, headers: Map<String, String> = emptyMap()) {
        content = WebContent.Url(url, headers.toMap())
        // App-driven loads get a fresh crash budget; internal re-drives keep the pending one, so they stay limited to one attempt.
        crashAutoReloadAttempted = false
        contentGeneration++
    }

    public fun loadHtml(html: String, baseUrl: String? = null) {
        content = WebContent.Html(html, baseUrl)
        crashAutoReloadAttempted = false
        contentGeneration++
    }

    public fun goBack() {
        _commands.tryEmit(WebViewCommand.GoBack)
    }

    public fun goForward() {
        _commands.tryEmit(WebViewCommand.GoForward)
    }

    public fun reload() {
        _commands.tryEmit(WebViewCommand.Reload)
    }

    public fun stopLoading() {
        _commands.tryEmit(WebViewCommand.StopLoading)
    }
}
