package com.kroegerama.kmp.kaiteki.webview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kroegerama.kmp.kaiteki.compose.modifier.checkerboard

/**
 * JS dialogs (`alert`, `confirm`, `prompt`) use platform UI unless [WebViewController.jsDialogHandler] is set;
 * `target="_blank"` links and permitted `window.open` calls load in the same web view.
 *
 * In [inspection mode][LocalInspectionMode] (e.g. IDE previews) a placeholder is rendered and no native
 * web view is created.
 *
 * @param handleBackNavigation When true, back navigates web history before leaving the screen.
 * Android intercepts system back while the web view can go back (requires an `OnBackPressedDispatcherOwner`,
 * e.g. a `ComponentActivity`); iOS enables the native back/forward edge-swipe gestures.
 */
@Composable
public fun WebView(
    controller: WebViewController,
    modifier: Modifier = Modifier,
    handleBackNavigation: Boolean = true,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.checkerboard(
                evenColor = Color.LightGray,
                oddColor = Color.White
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(controller.currentUrl ?: "WebView", fontSize = 24.sp, color = Color.Black)
        }
        return
    }
    PlatformWebView(
        controller = controller,
        modifier = modifier,
        handleBackNavigation = handleBackNavigation,
    )
}

@Composable
internal expect fun PlatformWebView(
    controller: WebViewController,
    modifier: Modifier,
    handleBackNavigation: Boolean,
)

/**
 * State a controller reports in [inspection mode][LocalInspectionMode], so previews of surrounding UI
 * (title bar, progress, back/forward buttons) have something to render. Ignored outside previews.
 */
@Immutable
public data class WebViewPreviewState(
    val title: String? = null,
    val loadingState: LoadingState = LoadingState.Finished,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
)

/**
 * [initialUrl] is consumed once. Changing [settings] creates a new controller and native view, losing history.
 * State does not survive activity recreation; use [rememberSaveableWebViewController] for that.
 *
 * @param previewState applied to the controller in [inspection mode][LocalInspectionMode] only.
 */
@Composable
public fun rememberWebViewController(
    initialUrl: String? = null,
    settings: WebViewSettings = WebViewSettings(),
    previewState: WebViewPreviewState = WebViewPreviewState(),
): WebViewController {
    val inspectionMode = LocalInspectionMode.current
    return remember(settings, previewState) {
        WebViewController(initialUrl, settings).apply {
            if (inspectionMode) applyPreviewState(previewState)
        }
    }
}

private const val WEB_VIEW_SAVER_MARKER = 1

/**
 * Like [rememberWebViewController], but restores navigation history (and on iOS scroll position) across
 * activity recreation and saveable-state disposal, falling back to reloading the last URL if native state is unavailable.
 * The URL fallback does not replay custom headers passed to [WebViewController.loadUrl].
 * On iOS the saved state is held in memory only and does not survive process termination.
 *
 * @param previewState applied to the controller in [inspection mode][LocalInspectionMode] only.
 */
@Composable
public fun rememberSaveableWebViewController(
    initialUrl: String? = null,
    settings: WebViewSettings = WebViewSettings(),
    previewState: WebViewPreviewState = WebViewPreviewState(),
): WebViewController {
    // Nothing to save or restore in a preview, and the saved state is platform-native.
    if (LocalInspectionMode.current) {
        return rememberWebViewController(initialUrl, settings, previewState)
    }
    return rememberSaveable(
        settings,
        saver = listSaver(
            save = { controller ->
                listOf(
                    WEB_VIEW_SAVER_MARKER,
                    controller.currentUrl,
                    controller.nativeStateProvider?.invoke() ?: controller.pendingRestore?.state,
                )
            },
            restore = { saved ->
                try {
                    check(saved.size == 3 && saved[0] == WEB_VIEW_SAVER_MARKER)
                    WebViewController(saved[1] as String?, settings).apply {
                        pendingRestore = (saved[2] as WebViewSavedState?)?.let { PendingRestore(it, contentGeneration) }
                    }
                } catch (_: Exception) {
                    null
                }
            },
        ),
    ) {
        WebViewController(initialUrl, settings)
    }
}

private fun WebViewController.applyPreviewState(state: WebViewPreviewState) {
    title = state.title
    loadingState = state.loadingState
    canGoBack = state.canGoBack
    canGoForward = state.canGoForward
}

@Preview(showBackground = true)
@Composable
private fun WebViewPreview() {
    val controller = rememberWebViewController(
        initialUrl = "https://example.com/",
        previewState = WebViewPreviewState(
            title = "Example Domain",
            loadingState = LoadingState.Loading(0.4f),
            canGoBack = true
        )
    )
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = controller.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            (controller.loadingState as? LoadingState.Loading)?.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            WebView(
                controller = controller,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
