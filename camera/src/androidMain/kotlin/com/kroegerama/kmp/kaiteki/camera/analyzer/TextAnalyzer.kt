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

private const val MAX_LINE_ANGLE_DEGREES = 45f

/** Smallest absolute difference between two angles in degrees, in `0..180`. */
private fun angularDifference(a: Float, b: Float): Float {
    val difference = (a - b).mod(360f)
    return if (difference > 180f) 360f - difference else difference
}

@ExperimentalKaitekiCameraApi
internal class TextAnalyzer(
    private val producer: ProducerScope<OCRResult>,
    private val minConfidence: Float
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
        // ML Kit returns bounding boxes in the upright coordinate space, while
        // InputImage reports the sensor-oriented buffer size.
        val uprightWidth = if (rotation % 180 == 0) image.width else image.height
        val uprightHeight = if (rotation % 180 == 0) image.height else image.width

        recognizer.process(image).addOnSuccessListener { text ->
            val blocks = text.textBlocks.asSequence().flatMap { it.lines }.mapNotNull { line ->
                if (line.confidence < minConfidence) return@mapNotNull null
                // Line.getAngle is relative to the sensor buffer, so upright text reads
                // ≈ -rotationDegrees (undocumented, verified on-device for display
                // rotations 0/90/180); rejects tilted and 180°-flipped lines.
                if (angularDifference(line.angle, -rotation.toFloat()) > MAX_LINE_ANGLE_DEGREES) return@mapNotNull null
                val box = line.boundingBox ?: return@mapNotNull null
                OCRResultBlock(
                    text = line.text,
                    confidence = line.confidence,
                    relativeX = box.left / uprightWidth.toFloat(),
                    relativeY = box.top / uprightHeight.toFloat(),
                    relativeWidth = box.width() / uprightWidth.toFloat(),
                    relativeHeight = box.height() / uprightHeight.toFloat(),
                )
            }.toList()
            producer.trySend(OCRResult(blocks = blocks))
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    fun close() {
        recognizer.close()
    }
}
