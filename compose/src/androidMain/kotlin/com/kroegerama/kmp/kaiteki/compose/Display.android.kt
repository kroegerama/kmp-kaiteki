package com.kroegerama.kmp.kaiteki.compose

import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView

@Composable
public fun rememberDisplayRotation(): Int {
    val view = LocalView.current
    val configuration = LocalConfiguration.current // triggers recomposition

    return remember(view, configuration) {
        view.display?.rotation ?: Surface.ROTATION_0
    }
}
