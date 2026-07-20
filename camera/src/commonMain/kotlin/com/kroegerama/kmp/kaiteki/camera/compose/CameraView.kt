package com.kroegerama.kmp.kaiteki.camera.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.sp
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.controller.CameraController
import com.kroegerama.kmp.kaiteki.camera.controller.PlatformCameraController
import com.kroegerama.kmp.kaiteki.compose.modifier.checkerboard

/**
 * Displays the live camera preview of the given [controller].
 *
 * In [inspection mode][LocalInspectionMode] (e.g. IDE previews) a plain placeholder
 * is rendered instead of the camera preview.
 */
@ExperimentalKaitekiCameraApi
@Composable
public fun CameraView(
    controller: CameraController,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.checkerboard(
                evenColor = Color.LightGray,
                oddColor = Color.White
            ),
            contentAlignment = Alignment.Center
        ) {
            Text("CameraView", fontSize = 32.sp, color = Color.Black)
        }
        return
    }
    PlatformCameraView(
        controller = controller as PlatformCameraController,
        modifier = modifier
    )
}

@ExperimentalKaitekiCameraApi
@Composable
internal expect fun PlatformCameraView(
    controller: PlatformCameraController,
    modifier: Modifier,
)
