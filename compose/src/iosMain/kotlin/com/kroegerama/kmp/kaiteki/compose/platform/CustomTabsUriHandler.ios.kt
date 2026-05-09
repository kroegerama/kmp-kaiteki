package com.kroegerama.kmp.kaiteki.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.uikit.LocalUIViewController
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIViewController

public class SafariViewControllerUriHandler @RememberInComposition constructor(
    private val uiViewController: UIViewController,
    private val animated: Boolean = true,
    private val completionHandler: (() -> Unit)? = null,
    private val decorator: SFSafariViewController.() -> Unit = {}
) : UriHandler {
    override fun openUri(uri: String) {
        val url = NSURL.URLWithString(uri) ?: return
        val viewController = SFSafariViewController(url).apply {
            decorator()
        }
        uiViewController.presentViewController(
            viewControllerToPresent = viewController,
            animated = animated,
            completion = completionHandler
        )
    }
}

@Composable
public actual fun rememberCustomTabsUriHandler(
    toolbarColor: Color,
    onToolbarColor: Color
): UriHandler {
    val uiViewController = LocalUIViewController.current
    return remember(uiViewController, toolbarColor, onToolbarColor) {
        SafariViewControllerUriHandler(
            uiViewController = uiViewController
        ) {
            preferredBarTintColor = toolbarColor.asUIColor()
            preferredControlTintColor = onToolbarColor.asUIColor()
        }
    }
}
