package com.kroegerama.kmp.kaiteki.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import java.awt.Desktop
import java.net.URI

public class DesktopUriHandler @RememberInComposition constructor(
) : UriHandler {

    private val fallbackUriHandler: (String) -> Unit by lazy {
        val name = System.getProperty("os.name")
        if (name?.startsWith("Linux") == true) {
            { Runtime.getRuntime().exec(arrayOf("xdg-open", it)) }
        } else {
            {}
        }
    }

    override fun openUri(uri: String) {
        val browsed = Desktop.isDesktopSupported() &&
                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) &&
                runCatching { Desktop.getDesktop().browse(URI(uri)) }.isSuccess
        if (!browsed) {
            fallbackUriHandler(uri)
        }
    }
}

@Composable
public actual fun rememberCustomTabsUriHandler(
    toolbarColor: Color,
    onToolbarColor: Color
): UriHandler {
    return remember { DesktopUriHandler() }
}
