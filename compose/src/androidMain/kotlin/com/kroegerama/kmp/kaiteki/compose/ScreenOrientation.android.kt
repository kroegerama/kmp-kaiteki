package com.kroegerama.kmp.kaiteki.compose

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
public fun RequestedOrientationEffect(
    orientation: Int
) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, orientation) {
        val oldOrientation = activity.requestedOrientation

        activity.requestedOrientation = orientation
        onDispose {
            activity.requestedOrientation = oldOrientation
        }
    }
}
