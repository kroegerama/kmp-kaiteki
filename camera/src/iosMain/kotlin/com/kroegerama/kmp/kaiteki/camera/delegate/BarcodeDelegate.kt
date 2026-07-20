package com.kroegerama.kmp.kaiteki.camera.delegate

import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.ProducerScope
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.darwin.NSObject

@ExperimentalKaitekiCameraApi
@OptIn(ExperimentalForeignApi::class)
internal class BarcodeDelegate(
    private val producer: ProducerScope<BarcodeResult>
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        didOutputMetadataObjects.filterIsInstance<AVMetadataMachineReadableCodeObject>().firstOrNull()?.let { readable ->
            val content = readable.stringValue ?: return@let
            val format = BarcodeFormat.fromPlatformBarcodeFormat(readable) ?: return@let

            producer.trySend(
                BarcodeResult(
                    format = format,
                    content = content
                )
            )
        }
    }
}
