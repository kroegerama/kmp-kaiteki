package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.runtime.Immutable

@Immutable
public data class OCRResult(
    val blocks: List<OCRResultBlock>
)

public data class OCRResultBlock(
    val text: String,
    val relativeX: Float,
    val relativeY: Float,
)
