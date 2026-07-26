package com.kroegerama.kmp.kaiteki.camera.analyzer

import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Vision.VNBarcodeObservation
import platform.Vision.VNBarcodeSymbology
import platform.Vision.VNBarcodeSymbologyAztec
import platform.Vision.VNBarcodeSymbologyCodabar
import platform.Vision.VNBarcodeSymbologyCode128
import platform.Vision.VNBarcodeSymbologyCode39
import platform.Vision.VNBarcodeSymbologyCode93
import platform.Vision.VNBarcodeSymbologyDataMatrix
import platform.Vision.VNBarcodeSymbologyEAN13
import platform.Vision.VNBarcodeSymbologyEAN8
import platform.Vision.VNBarcodeSymbologyITF14
import platform.Vision.VNBarcodeSymbologyPDF417
import platform.Vision.VNBarcodeSymbologyQR
import platform.Vision.VNBarcodeSymbologyUPCE
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler

@ExperimentalKaitekiCameraApi
private val BarcodeFormat.symbology: VNBarcodeSymbology
    get() = when (this) {
        BarcodeFormat.AZTEC -> VNBarcodeSymbologyAztec
        BarcodeFormat.CODE_128 -> VNBarcodeSymbologyCode128
        BarcodeFormat.CODE_39 -> VNBarcodeSymbologyCode39
        BarcodeFormat.CODE_93 -> VNBarcodeSymbologyCode93
        BarcodeFormat.CODABAR -> VNBarcodeSymbologyCodabar
        BarcodeFormat.DATA_MATRIX -> VNBarcodeSymbologyDataMatrix
        BarcodeFormat.EAN_13 -> VNBarcodeSymbologyEAN13
        BarcodeFormat.EAN_8 -> VNBarcodeSymbologyEAN8
        BarcodeFormat.ITF -> VNBarcodeSymbologyITF14
        BarcodeFormat.PDF_417 -> VNBarcodeSymbologyPDF417
        BarcodeFormat.QR_CODE -> VNBarcodeSymbologyQR
        // UPC-A = EAN13 with stripped leading zero
        BarcodeFormat.UPC_A -> VNBarcodeSymbologyEAN13
        BarcodeFormat.UPC_E -> VNBarcodeSymbologyUPCE
    }

@ExperimentalKaitekiCameraApi
private fun barcodeFormat(symbology: VNBarcodeSymbology, content: String): BarcodeFormat? = when (symbology) {
    VNBarcodeSymbologyAztec -> BarcodeFormat.AZTEC
    VNBarcodeSymbologyCodabar -> BarcodeFormat.CODABAR
    VNBarcodeSymbologyCode39 -> BarcodeFormat.CODE_39
    VNBarcodeSymbologyCode93 -> BarcodeFormat.CODE_93
    VNBarcodeSymbologyCode128 -> BarcodeFormat.CODE_128
    VNBarcodeSymbologyDataMatrix -> BarcodeFormat.DATA_MATRIX
    VNBarcodeSymbologyEAN8 -> BarcodeFormat.EAN_8
    VNBarcodeSymbologyEAN13 -> {
        if (content.length == 13 && content[0] == '0') {
            BarcodeFormat.UPC_A
        } else {
            BarcodeFormat.EAN_13
        }
    }

    VNBarcodeSymbologyITF14 -> BarcodeFormat.ITF
    VNBarcodeSymbologyPDF417 -> BarcodeFormat.PDF_417
    VNBarcodeSymbologyQR -> BarcodeFormat.QR_CODE
    VNBarcodeSymbologyUPCE -> BarcodeFormat.UPC_E
    else -> null
}

/**
 * Runs Vision barcode detection on [VNImageRequestHandler]s, e.g. still images.
 * Live camera scanning uses AVFoundation metadata instead, see `CameraController.bindBarcodeAnalyzerFlow`.
 *
 * @param formats barcode formats to detect; must not be empty.
 */
@ExperimentalKaitekiCameraApi
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public class BarcodeProcessor(
    vararg formats: BarcodeFormat,
) {
    private val requestSymbologies: List<VNBarcodeSymbology> = when (formats.size) {
        0 -> error("formats must not be empty")
        else -> formats.map { it.symbology }.distinct()
    }

    /**
     * Scans the image of [handler] and returns all detected barcodes with a supported format and payload.
     */
    public suspend fun scan(handler: VNImageRequestHandler): List<BarcodeResult> = withContext(Dispatchers.Default) {
        autoreleasepool {
            val request = VNDetectBarcodesRequest(completionHandler = null).apply {
                symbologies = requestSymbologies
            }
            memScoped {
                val nsError = alloc<ObjCObjectVar<NSError?>>()
                if (!handler.performRequests(listOf(request), nsError.ptr)) {
                    error("barcode detection failed: ${nsError.value?.localizedDescription}")
                }
            }
            val observations = request.results?.filterIsInstance<VNBarcodeObservation>().orEmpty()
            observations.mapNotNull { observation ->
                val content = observation.payloadStringValue ?: return@mapNotNull null
                val format = barcodeFormat(observation.symbology, content) ?: return@mapNotNull null
                BarcodeResult(
                    format = format,
                    content = content
                )
            }
        }
    }
}
