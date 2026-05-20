package com.kroegerama.kmp.kaiteki.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.controller.CameraController

@ExperimentalKaitekiCameraApi
@Composable
public expect fun CameraView(
    controller: CameraController,
    modifier: Modifier,
)
