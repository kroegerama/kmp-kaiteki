package com.kroegerama.kmp.kaiteki.compose.modifier

import androidx.compose.ui.Modifier

/**
 * Keeps the screen turned on while the modified composable is in the composition. The normal
 * display timeout is restored once it leaves the composition.
 *
 * On Android this delegates to the platform `keepScreenOn` flag, on iOS it toggles
 * `UIApplication.idleTimerDisabled`.
 */
public expect fun Modifier.keepScreenOn(): Modifier
