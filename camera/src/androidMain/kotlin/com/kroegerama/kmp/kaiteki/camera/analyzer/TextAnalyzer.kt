package com.kroegerama.kmp.kaiteki.camera.analyzer

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.common.InputImage
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import com.kroegerama.kmp.kaiteki.camera.model.containsRect
import com.kroegerama.kmp.kaiteki.camera.model.relativeRect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.launch

@ExperimentalKaitekiCameraApi
@kotlin.OptIn(DelicateCoroutinesApi::class)
internal class TextAnalyzer(
    private val producer: ProducerScope<OCRResult>,
    minConfidence: Float,
    private val roiProvider: () -> Rect?
) : ImageAnalysis.Analyzer {
    private val processor = OCRProcessor(minConfidence)

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
                val result = processor.recognize(image)
                val filtered = if (roi == null) {
                    result
                } else {
                    result.copy(blocks = result.blocks.filter { roi.containsRect(it.relativeRect) })
                }
                producer.trySend(filtered)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // recognition failed; drop the frame
            } finally {
                imageProxy.close()
            }
        }
    }

    fun close() {
        processor.close()
    }
}
