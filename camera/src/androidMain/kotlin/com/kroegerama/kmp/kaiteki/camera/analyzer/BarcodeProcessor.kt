package com.kroegerama.kmp.kaiteki.camera.analyzer

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.common.InputImage
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import kotlinx.coroutines.tasks.await

private const val DEFAULT_MAX_SUPPORTED_ZOOM_RATIO = 5f

/**
 * Runs ML Kit barcode scanning on [InputImage]s, e.g. camera frames or [InputImage.fromFilePath].
 * Reusable for multiple images; call [close] to release the underlying scanner.
 *
 * @param formats barcode formats to detect; must not be empty.
 * @param zoomCallback invoked with ML Kit zoom suggestions during live scanning;
 * returns whether the zoom ratio was applied. Omit for still images.
 * @param maxSupportedZoomRatio highest zoom ratio the [zoomCallback] can apply.
 */
@ExperimentalKaitekiCameraApi
public class BarcodeProcessor(
    vararg formats: BarcodeFormat,
    zoomCallback: ((Float) -> Boolean)? = null,
    maxSupportedZoomRatio: Float = DEFAULT_MAX_SUPPORTED_ZOOM_RATIO,
) : AutoCloseable {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().run {
            val platformFormats = formats.map(BarcodeFormat::platformBarcodeFormat)
            when (platformFormats.size) {
                0 -> error("formats must not be empty")
                1 -> setBarcodeFormats(platformFormats.first())
                else -> setBarcodeFormats(platformFormats.first(), *platformFormats.drop(1).toIntArray())
            }
            if (zoomCallback != null) {
                setZoomSuggestionOptions(
                    ZoomSuggestionOptions.Builder(zoomCallback)
                        .setMaxSupportedZoomRatio(maxSupportedZoomRatio)
                        .build()
                )
            }
            build()
        }
    )

    /**
     * Scans [image] and returns all detected barcodes with a supported format and raw value.
     */
    public suspend fun scan(image: InputImage): List<BarcodeResult> {
        val rotation = image.rotationDegrees
        // ML Kit returns bounding boxes in the upright coordinate space, while
        // InputImage reports the sensor-oriented buffer size.
        val uprightWidth = if (rotation % 180 == 0) image.width else image.height
        val uprightHeight = if (rotation % 180 == 0) image.height else image.width

        return scanner.process(image).await().mapNotNull { barcode ->
            val format = BarcodeFormat.fromPlatformBarcodeFormat(barcode.format) ?: return@mapNotNull null
            val content = barcode.rawValue ?: return@mapNotNull null
            val box = barcode.boundingBox
            if (box == null) {
                // No bounding box available; report the full frame.
                BarcodeResult(
                    format = format,
                    content = content,
                    relativeX = 0f,
                    relativeY = 0f,
                    relativeWidth = 1f,
                    relativeHeight = 1f,
                )
            } else {
                BarcodeResult(
                    format = format,
                    content = content,
                    relativeX = box.left / uprightWidth.toFloat(),
                    relativeY = box.top / uprightHeight.toFloat(),
                    relativeWidth = box.width() / uprightWidth.toFloat(),
                    relativeHeight = box.height() / uprightHeight.toFloat(),
                )
            }
        }
    }

    override fun close() {
        scanner.close()
    }
}
