package com.kroegerama.kmp.kaiteki.compose.platform

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler

/**
 * Remembers a [UriHandler] that opens links in an in-app browser tab
 * (Chrome Custom Tabs on Android, `SFSafariViewController` on iOS).
 *
 * @param toolbarColor Background color of the browser toolbar.
 * @param onToolbarColor Tint for the toolbar controls. **iOS only** — on Android,
 *   Chrome Custom Tabs derives the toolbar foreground color automatically from
 *   [toolbarColor] and exposes no API to set it directly, so this parameter is ignored.
 */
@Composable
public expect fun rememberCustomTabsUriHandler(
    toolbarColor: Color = MaterialTheme.colorScheme.surface,
    onToolbarColor: Color = MaterialTheme.colorScheme.onSurface
): UriHandler
