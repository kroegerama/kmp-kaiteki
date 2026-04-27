package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.runtime.Immutable

@Immutable
public data class BarcodeResult(
    val format: BarcodeFormat,
    val content: String
)
