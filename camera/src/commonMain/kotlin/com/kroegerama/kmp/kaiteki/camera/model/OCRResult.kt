package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.runtime.Immutable
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi

@ExperimentalKaitekiCameraApi
@Immutable
public data class OCRResult(
    val blocks: List<OCRResultBlock>
)

@ExperimentalKaitekiCameraApi
public data class OCRResultBlock(
    val text: String,
    val relativeX: Float,
    val relativeY: Float,
)
