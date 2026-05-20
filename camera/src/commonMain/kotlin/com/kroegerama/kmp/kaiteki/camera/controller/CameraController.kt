package com.kroegerama.kmp.kaiteki.camera.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.extensions.CameraControllerExtension
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat

@ExperimentalKaitekiCameraApi
@Immutable
public expect class CameraController {
    public var zoomRatio: Float
    public var torchEnabled: Boolean
    public val torchAvailable: Boolean

    public fun toggleTorch()
    public fun focus(coords: Offset)

    internal fun clear()
}

@ExperimentalKaitekiCameraApi
@Composable
public expect fun rememberCameraController(): CameraController

@ExperimentalKaitekiCameraApi
@Composable
public expect fun rememberBarcodeExtension(
    cameraController: CameraController,
    barcodeFormats: List<BarcodeFormat>
): CameraControllerExtension.BarcodeExtension

@ExperimentalKaitekiCameraApi
@Composable
public expect fun rememberOcrExtension(
    cameraController: CameraController,
): CameraControllerExtension.OcrExtension
