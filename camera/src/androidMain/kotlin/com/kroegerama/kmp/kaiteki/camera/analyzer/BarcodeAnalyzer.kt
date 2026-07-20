package com.kroegerama.kmp.kaiteki.camera.analyzer

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.common.InputImage
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import kotlinx.coroutines.channels.ProducerScope

@ExperimentalKaitekiCameraApi
internal class BarcodeAnalyzer(
    private val producer: ProducerScope<BarcodeResult>,
    zoomCallback: (Float) -> Boolean,
    vararg formats: BarcodeFormat
) : ImageAnalysis.Analyzer {
    private val barcodeScannerOptions = BarcodeScannerOptions.Builder().run {
        val formats = formats.map(BarcodeFormat::platformBarcodeFormat)
        when (formats.size) {
            0 -> error("formats must not be empty")
            1 -> setBarcodeFormats(formats.first())
            else -> setBarcodeFormats(formats.first(), *formats.drop(1).toIntArray())
        }
    }.setZoomSuggestionOptions(
        ZoomSuggestionOptions.Builder(zoomCallback)
            .setMaxSupportedZoomRatio(5f)
            .build()
    ).build()

    private val scanner = BarcodeScanning.getClient(barcodeScannerOptions)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        scanner.process(image).addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.let { barcode ->
                val format = BarcodeFormat.fromPlatformBarcodeFormat(barcode.format) ?: return@let
                val content = barcode.rawValue ?: return@let
                producer.trySend(
                    BarcodeResult(
                        format = format,
                        content = content
                    )
                )
            }
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    fun close() {
        scanner.close()
    }
}
