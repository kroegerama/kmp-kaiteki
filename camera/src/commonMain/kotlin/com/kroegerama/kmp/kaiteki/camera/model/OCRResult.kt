package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.runtime.Immutable
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi

/**
 * Result of a single OCR pass over one camera frame.
 */
@ExperimentalKaitekiCameraApi
@Immutable
public data class OCRResult(
    val blocks: List<OCRResultBlock>
)

/**
 * A single recognized line of text with its recognition [confidence] in `0..1`.
 * Coordinates are normalized to `0..1`, relative to the upright frame with top-left origin.
 */
@ExperimentalKaitekiCameraApi
public data class OCRResultBlock(
    val text: String,
    val confidence: Float,
    val relativeX: Float,
    val relativeY: Float,
    val relativeWidth: Float,
    val relativeHeight: Float,
)
