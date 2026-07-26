package com.kroegerama.kmp.kaiteki.camera.analyzer

import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.controller.DEFAULT_MIN_OCR_CONFIDENCE
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResultBlock
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRect
import platform.Foundation.NSError
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * Runs Vision text recognition on [VNImageRequestHandler]s, e.g. camera frames or still images.
 *
 * @param minConfidence lines below this recognition confidence (`0..1`) are discarded.
 * @param minimumTextHeight minimum text height relative to the image height; `null` keeps the Vision default.
 * @param regionOfInterest normalized, bottom-left-origin region to recognize text in; `null` uses the full image.
 */
@ExperimentalKaitekiCameraApi
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public class OCRProcessor(
    private val minConfidence: Float = DEFAULT_MIN_OCR_CONFIDENCE,
    private val minimumTextHeight: Float? = null,
    private val regionOfInterest: CValue<CGRect>? = null,
) {
    /**
     * Recognizes text in the image of [handler].
     */
    public suspend fun recognize(handler: VNImageRequestHandler): OCRResult = withContext(Dispatchers.Default) {
        autoreleasepool {
            val request = VNRecognizeTextRequest(completionHandler = null).apply {
                recognitionLevel = VNRequestTextRecognitionLevelAccurate
                usesLanguageCorrection = true
                this@OCRProcessor.minimumTextHeight?.let { minimumTextHeight = it }
                this@OCRProcessor.regionOfInterest?.let { regionOfInterest = it }
            }
            memScoped {
                val nsError = alloc<ObjCObjectVar<NSError?>>()
                if (!handler.performRequests(listOf(request), nsError.ptr)) {
                    error("text recognition failed: ${nsError.value?.localizedDescription}")
                }
            }
            val observations = request.results?.filterIsInstance<VNRecognizedTextObservation>().orEmpty()
            val blocks = observations.mapNotNull { observation ->
                val candidate = observation.topCandidates(1u).firstOrNull() as? VNRecognizedText ?: return@mapNotNull null
                if (candidate.confidence < minConfidence) return@mapNotNull null
                // Vision uses a normalized, bottom-left-origin coordinate space; convert to top-left origin.
                observation.boundingBox.useContents {
                    OCRResultBlock(
                        text = candidate.string,
                        confidence = candidate.confidence,
                        relativeX = origin.x.toFloat(),
                        relativeY = (1.0 - origin.y - size.height).toFloat(),
                        relativeWidth = size.width.toFloat(),
                        relativeHeight = size.height.toFloat(),
                    )
                }
            }
            OCRResult(blocks = blocks)
        }
    }
}
