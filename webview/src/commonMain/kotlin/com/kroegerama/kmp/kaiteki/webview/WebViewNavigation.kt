package com.kroegerama.kmp.kaiteki.webview

public enum class NavigationDecision {
    Allow, Cancel
}

public enum class NavigationType {
    LinkActivated, FormSubmitted, Other
}

public data class NavigationRequest(
    val url: String,
    val isMainFrame: Boolean,
    /** Reliable on iOS. On Android only [NavigationType.LinkActivated] vs [NavigationType.Other] (gesture heuristic). */
    val type: NavigationType,
    /** Android API 24+ only; always false on iOS. */
    val isRedirect: Boolean = false,
)

/** Schemes a web view loads itself; anything else can only be opened by another app. */
private val internallyHandledSchemes = setOf(
    "http", "https", "file", "data", "about", "blob", "javascript", "content", "ws", "wss"
)

/** True when [url] names a scheme the web view cannot load and [WebViewSettings.openExternalSchemes] is on. */
internal fun WebViewSettings.shouldOpenExternally(url: String): Boolean {
    if (!openExternalSchemes) return false
    val scheme = url.substringBefore(':', "").lowercase()
    if (scheme.isEmpty() || !scheme[0].isLetter()) return false
    return scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' } && scheme !in internallyHandledSchemes
}

public fun interface NavigationInterceptor {
    /**
     * Called on the main thread for navigations originating inside the page (links, JS, redirects);
     * not for controller-initiated loads, reload or back/forward. Must return quickly, as this blocks navigation.
     * Subframe navigations are only reliably delivered on iOS; check [NavigationRequest.isMainFrame].
     */
    public fun onNavigation(request: NavigationRequest): NavigationDecision
}
