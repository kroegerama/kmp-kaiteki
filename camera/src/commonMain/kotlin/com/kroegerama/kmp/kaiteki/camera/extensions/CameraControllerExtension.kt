package com.kroegerama.kmp.kaiteki.camera.extensions

import androidx.compose.runtime.Immutable
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import kotlinx.coroutines.flow.Flow

@Immutable
public sealed interface CameraControllerExtension {
    @Immutable
    public class BarcodeExtension internal constructor(
        public val formats: List<BarcodeFormat>,
        public val barcodeResults: Flow<BarcodeResult>
    ) : CameraControllerExtension

    @Immutable
    public class OcrExtension internal constructor(
        public val ocrResults: Flow<OCRResult>
    ) : CameraControllerExtension
}
