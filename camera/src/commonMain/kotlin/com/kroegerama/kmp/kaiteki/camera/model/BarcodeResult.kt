package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.runtime.Immutable
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi

@ExperimentalKaitekiCameraApi
@Immutable
public data class BarcodeResult(
    val format: BarcodeFormat,
    val content: String
)
