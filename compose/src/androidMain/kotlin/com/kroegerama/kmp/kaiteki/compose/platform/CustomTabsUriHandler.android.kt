package com.kroegerama.kmp.kaiteki.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import com.kroegerama.kmp.kaiteki.compose.rememberChromeCustomTabUriHandler

@Composable
public actual fun rememberCustomTabsUriHandler(
    toolbarColor: Color,
    onToolbarColor: Color
): UriHandler = rememberChromeCustomTabUriHandler(
    toolbarColor = toolbarColor,
    secondaryToolbarColor = toolbarColor
)
