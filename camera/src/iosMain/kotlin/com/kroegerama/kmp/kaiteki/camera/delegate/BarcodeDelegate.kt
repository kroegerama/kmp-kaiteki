package com.kroegerama.kmp.kaiteki.camera.delegate

import androidx.compose.ui.geometry.Rect
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.controller.AnalysisGeometry
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import com.kroegerama.kmp.kaiteki.camera.model.containsRect
import com.kroegerama.kmp.kaiteki.camera.model.relativeRect
import com.kroegerama.kmp.kaiteki.camera.model.rotateNormalized
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.ProducerScope
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.darwin.NSObject

@ExperimentalKaitekiCameraApi
@OptIn(ExperimentalForeignApi::class)
internal class BarcodeDelegate(
    private val producer: ProducerScope<BarcodeResult>,
    private val geometryProvider: () -> AnalysisGeometry,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        val geometry = geometryProvider()
        val rotationDegrees = geometry.uprightRotationDegrees
        val roi = geometry.roi?.rotateNormalized(rotationDegrees)
        didOutputMetadataObjects.asSequence()
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .mapNotNull { readable ->
                val content = readable.stringValue ?: return@mapNotNull null
                val format = BarcodeFormat.fromPlatformBarcodeFormat(readable) ?: return@mapNotNull null
                // bounds are normalized in the unrotated output coordinate space;
                // rotate them into the upright frame.
                val box = readable.bounds.useContents {
                    Rect(
                        origin.x.toFloat(),
                        origin.y.toFloat(),
                        (origin.x + size.width).toFloat(),
                        (origin.y + size.height).toFloat(),
                    )
                }.rotateNormalized(rotationDegrees)

                BarcodeResult(
                    format = format,
                    content = content,
                    relativeX = box.left,
                    relativeY = box.top,
                    relativeWidth = box.width,
                    relativeHeight = box.height,
                )
            }
            .firstOrNull { roi == null || roi.containsRect(it.relativeRect) }
            ?.let(producer::trySend)
    }
}
