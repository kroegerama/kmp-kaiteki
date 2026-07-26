package com.kroegerama.kmp.kaiteki.camera.analyzer

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.launch

@ExperimentalKaitekiCameraApi
@kotlin.OptIn(DelicateCoroutinesApi::class)
internal class TextAnalyzer(
    private val producer: ProducerScope<OCRResult>,
    minConfidence: Float
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
        // ATOMIC keeps the cleanup in `finally` running even if the flow was
        // just cancelled; the image proxy must be closed either way.
        producer.launch(start = CoroutineStart.ATOMIC) {
            try {
                producer.trySend(processor.recognize(image))
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
