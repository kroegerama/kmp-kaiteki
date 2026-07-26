package com.kroegerama.kmp.kaiteki.camera.analyzer

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.controller.DEFAULT_MIN_OCR_CONFIDENCE
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResultBlock
import kotlinx.coroutines.tasks.await

private const val DEFAULT_MAX_LINE_ANGLE_DEGREES = 45f

/** Smallest absolute difference between two angles in degrees, in `0..180`. */
private fun angularDifference(a: Float, b: Float): Float {
    val difference = (a - b).mod(360f)
    return if (difference > 180f) 360f - difference else difference
}

/**
 * Runs ML Kit text recognition on [InputImage]s, e.g. camera frames or [InputImage.fromFilePath].
 * Reusable for multiple images; call [close] to release the underlying recognizer.
 *
 * @param minConfidence lines below this recognition confidence (`0..1`) are discarded.
 * @param maxLineAngleDegrees lines tilted more than this from upright are discarded;
 * use [Float.POSITIVE_INFINITY] to keep all lines.
 */
@ExperimentalKaitekiCameraApi
public class OCRProcessor(
    private val minConfidence: Float = DEFAULT_MIN_OCR_CONFIDENCE,
    private val maxLineAngleDegrees: Float = DEFAULT_MAX_LINE_ANGLE_DEGREES,
) : AutoCloseable {
    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    /**
     * Recognizes text in [image].
     */
    public suspend fun recognize(image: InputImage): OCRResult {
        val rotation = image.rotationDegrees
        // ML Kit returns bounding boxes in the upright coordinate space, while
        // InputImage reports the sensor-oriented buffer size.
        val uprightWidth = if (rotation % 180 == 0) image.width else image.height
        val uprightHeight = if (rotation % 180 == 0) image.height else image.width

        val text = recognizer.process(image).await()
        val blocks = text.textBlocks.asSequence().flatMap { it.lines }.mapNotNull { line ->
            if (line.confidence < minConfidence) return@mapNotNull null
            // Line.getAngle is relative to the sensor buffer, so upright text reads
            // ≈ -rotationDegrees (undocumented, verified on-device for display
            // rotations 0/90/180); rejects tilted and 180°-flipped lines.
            if (angularDifference(line.angle, -rotation.toFloat()) > maxLineAngleDegrees) return@mapNotNull null
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
        return OCRResult(blocks = blocks)
    }

    override fun close() {
        recognizer.close()
    }
}
