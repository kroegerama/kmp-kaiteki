package com.kroegerama.kmp.kaiteki.webview

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class WebViewSettings(
    val javaScriptEnabled: Boolean = true,
    /** Allows `window.open` without a user gesture. Opened windows load in the same web view; iOS ignores `window.open` without a URL. */
    val javaScriptCanOpenWindowsAutomatically: Boolean = false,
    val mediaPlaybackRequiresUserGesture: Boolean = true,
    /** When false on iOS, a viewport script enforcing `user-scalable=no` is injected. */
    val zoomEnabled: Boolean = false,
    val userAgent: String? = null,
    /** Android only. WKWebView always has DOM storage enabled; ignored on iOS. */
    val domStorageEnabled: Boolean = true,
    /**
     * Painted before the page supplies a background of its own, replacing the platform default white.
     * A translucent color makes the iOS web view non-opaque, which also leaves the overscroll area
     * uncolored and forces a dark scroll indicator style.
     */
    val backgroundColor: Color? = null,
    /**
     * iOS only. When false, the web view does not scroll itself and pan gestures starting on it are offered
     * to Compose parents first. Ignored on Android.
     */
    val scrollEnabled: Boolean = true,
    /**
     * iOS only. When true, VoiceOver traverses the web content itself instead of Compose semantics.
     * Setting this to false hides the page from accessibility services unless the caller supplies its own
     * `Modifier.semantics`. Ignored on Android.
     */
    val nativeAccessibilityEnabled: Boolean = true,
    /** iOS only. Lets Safari Web Inspector attach to the web view. Requires iOS 16.4; ignored below that and on Android. */
    val inspectable: Boolean = false,
    /**
     * Hands URLs the web view cannot load itself (`mailto:`, `tel:`, custom app schemes) to
     * `LocalUriHandler` and cancels the navigation. When false, such a navigation fails instead.
     * Applies to navigations started by the page; [WebViewController.loadUrl] is never redirected.
     * A [NavigationInterceptor] still sees the navigation first and can cancel it.
     */
    val openExternalSchemes: Boolean = true,
)
