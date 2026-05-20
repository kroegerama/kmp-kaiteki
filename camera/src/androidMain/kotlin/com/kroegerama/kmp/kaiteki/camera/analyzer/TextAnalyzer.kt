package com.kroegerama.kmp.kaiteki.camera.analyzer

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResultBlock
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.Executor

@ExperimentalKaitekiCameraApi
public fun ImageAnalysis.bindTextAnalyzerFlow(
    executor: Executor
): Flow<OCRResult> = callbackFlow {
    val analyzer = TextAnalyzer(this)
    setAnalyzer(executor, analyzer)
    awaitClose { clearAnalyzer() }
}.distinctUntilChanged()

@ExperimentalKaitekiCameraApi
private class TextAnalyzer(
    private val producer: ProducerScope<OCRResult>
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        recognizer.process(image).addOnSuccessListener { text ->
            val blocks = text.textBlocks.map { textBlock ->
                val x = textBlock.boundingBox?.top?.toFloat() ?: 0f
                val y = textBlock.boundingBox?.left?.toFloat() ?: 0f
                OCRResultBlock(
                    text = textBlock.text,
                    relativeX = x / image.width,
                    relativeY = y / image.height
                )
            }
            producer.trySend(
                OCRResult(
                    blocks = blocks
                )
            )
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    fun close() {
        recognizer.close()
    }
}
