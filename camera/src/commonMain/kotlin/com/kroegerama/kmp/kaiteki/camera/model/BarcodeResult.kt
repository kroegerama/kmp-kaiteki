package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.runtime.Immutable
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi

/**
 * A single detected barcode with its bounding box.
 * Coordinates are normalized to `0..1`, relative to the upright frame with top-left origin.
 */
@ExperimentalKaitekiCameraApi
@Immutable
public data class BarcodeResult(
    val format: BarcodeFormat,
    val content: String,
    val relativeX: Float,
    val relativeY: Float,
    val relativeWidth: Float,
    val relativeHeight: Float,
)
