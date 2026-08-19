package com.kroegerama.kmp.kaiteki.camera.analyzer

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.common.InputImage
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import com.kroegerama.kmp.kaiteki.camera.model.containsRect
import com.kroegerama.kmp.kaiteki.camera.model.relativeRect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.launch

@ExperimentalKaitekiCameraApi
@kotlin.OptIn(DelicateCoroutinesApi::class)
internal class BarcodeAnalyzer(
    private val producer: ProducerScope<BarcodeResult>,
    zoomCallback: (Float) -> Boolean,
    private val roiProvider: () -> Rect?,
    vararg formats: BarcodeFormat
) : ImageAnalysis.Analyzer {
    private val processor = BarcodeProcessor(
        formats = formats,
        // ML Kit suggests zooming without the triggering barcode's position, so suggestions
        // cannot be filtered by the region; suppress them while a region is active.
        zoomCallback = { ratio -> roiProvider() == null && zoomCallback(ratio) }
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val roi = uprightAnalysisRoi(roiProvider(), imageProxy)
        // ATOMIC keeps the cleanup in `finally` running even if the flow was
        // just cancelled; the image proxy must be closed either way.
        producer.launch(start = CoroutineStart.ATOMIC) {
            try {
                processor.scan(image)
                    .firstOrNull { roi == null || roi.containsRect(it.relativeRect) }
                    ?.let(producer::trySend)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // scan failed; drop the frame
            } finally {
                imageProxy.close()
            }
        }
    }

    fun close() {
        processor.close()
    }
}
