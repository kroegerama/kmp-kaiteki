package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.ui.geometry.Rect
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi

/** Rotates a normalized (`0..1`, top-left origin) rect clockwise by [degrees], a multiple of 90. */
internal fun Rect.rotateNormalized(degrees: Int): Rect = when (((degrees % 360) + 360) % 360) {
    90 -> Rect(1f - bottom, left, 1f - top, right)
    180 -> Rect(1f - right, 1f - bottom, 1f - left, 1f - top)
    270 -> Rect(top, 1f - right, bottom, 1f - left)
    else -> this
}

internal fun Rect.containsRect(other: Rect): Boolean =
    other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom

@ExperimentalKaitekiCameraApi
internal val BarcodeResult.relativeRect: Rect
    get() = Rect(relativeX, relativeY, relativeX + relativeWidth, relativeY + relativeHeight)

@ExperimentalKaitekiCameraApi
internal val OCRResultBlock.relativeRect: Rect
    get() = Rect(relativeX, relativeY, relativeX + relativeWidth, relativeY + relativeHeight)
