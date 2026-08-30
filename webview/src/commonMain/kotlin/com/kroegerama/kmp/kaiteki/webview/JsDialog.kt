package com.kroegerama.kmp.kaiteki.webview

import androidx.compose.runtime.Immutable

/** A JavaScript dialog request (`alert`, `confirm` or `prompt`) raised by the page. */
@Immutable
public sealed interface JsDialog {
    /** Dialog text passed by the page. */
    public val message: String

    /**
     * Identifies the requesting frame: its URL on Android, its security origin (`scheme://host[:port]`)
     * on iOS; may be empty (e.g. opaque origins). Display it so pages cannot impersonate the app.
     */
    public val sourceUrl: String

    public data class Alert(override val message: String, override val sourceUrl: String) : JsDialog
    public data class Confirm(override val message: String, override val sourceUrl: String) : JsDialog
    public data class Prompt(override val message: String, val defaultValue: String, override val sourceUrl: String) : JsDialog
}

/**
 * Shows JavaScript dialogs in place of the platform default UI, e.g. as Compose dialogs.
 * Called on the main thread. Page JavaScript is blocked until the returned value is delivered, on
 * Android in every WebView of the app (they share one renderer process), so deliver promptly.
 * Cancellation (e.g. the [WebView] leaving composition mid-dialog) and thrown exceptions count as Cancel.
 * `beforeunload` confirmations are not routed here (Android keeps the framework dialog; iOS has no such callback).
 */
public interface JsDialogHandler {
    /** Show the message; return once the user dismissed it. */
    public suspend fun onAlert(dialog: JsDialog.Alert)

    /** @return true for OK, false for Cancel. */
    public suspend fun onConfirm(dialog: JsDialog.Confirm): Boolean

    /** @return the entered text for OK, null for Cancel. */
    public suspend fun onPrompt(dialog: JsDialog.Prompt): String?
}
