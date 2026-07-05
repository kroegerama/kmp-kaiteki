package com.kroegerama.kmp.kaiteki.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import com.kroegerama.kmp.kaiteki.compose.rememberChromeCustomTabUriHandler

/**
 * onToolbarColor is intentionally unused: Chrome Custom Tabs derives the toolbar
 * foreground color from toolbarColor and offers no API to set it directly.
 */
@Composable
public actual fun rememberCustomTabsUriHandler(
    toolbarColor: Color,
    onToolbarColor: Color
): UriHandler = rememberChromeCustomTabUriHandler(
    toolbarColor = toolbarColor,
    secondaryToolbarColor = toolbarColor
)
