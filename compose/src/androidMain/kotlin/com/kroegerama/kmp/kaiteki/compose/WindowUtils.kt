package com.kroegerama.kmp.kaiteki.compose

import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
public fun ImmersiveModeEffect() {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    DisposableEffect(view) {
        val window = activity.window ?: return@DisposableEffect onDispose { }
        val insetsController = WindowCompat.getInsetsController(window, view)
        val systemBars = WindowInsetsCompat.Type.systemBars()

        insetsController.hide(systemBars)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            insetsController.show(systemBars)
        }
    }
}

@Composable
public fun ScreenBrightnessEffect(
    brightness: Float = 1f
) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(Unit) {
        val window = activity.window
        val oldBrightness = window.screenBrightness
        window.screenBrightness = brightness
        onDispose {
            window.screenBrightness = oldBrightness
        }
    }
}

private var Window.screenBrightness: Float
    get() = attributes.screenBrightness
    set(value) {
        attributes = attributes.apply {
            screenBrightness = value
        }
    }
