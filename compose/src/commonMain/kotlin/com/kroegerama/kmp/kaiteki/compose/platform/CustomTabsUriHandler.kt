package com.kroegerama.kmp.kaiteki.compose.platform

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler

@Composable
public expect fun rememberCustomTabsUriHandler(
    toolbarColor: Color = MaterialTheme.colorScheme.surface,
    onToolbarColor: Color = MaterialTheme.colorScheme.onSurface
): UriHandler
