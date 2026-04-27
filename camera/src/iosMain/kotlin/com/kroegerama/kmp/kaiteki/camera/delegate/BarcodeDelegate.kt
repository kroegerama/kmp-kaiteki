package com.kroegerama.kmp.kaiteki.camera.delegate

import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import com.kroegerama.kmp.kaiteki.camera.withConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_attr_make_with_qos_class
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.posix.QOS_CLASS_USER_INITIATED

@OptIn(ExperimentalForeignApi::class)
public fun AVCaptureSession.bindBarcodeDelegateFlow(
    formats: List<BarcodeFormat>,
    sessionQueue: dispatch_queue_t,
): Flow<BarcodeResult> = callbackFlow {
    val metadataOutput = AVCaptureMetadataOutput()
    val delegate = BarcodeDelegate(this)
    val stableRef = StableRef.create(delegate)

    dispatch_async(sessionQueue) {
        withConfiguration {
            if (canAddOutput(metadataOutput)) {
                addOutput(metadataOutput)
                metadataOutput.metadataObjectTypes = formats.map { it.platformBarcodeFormat }
            }
            val queue = dispatch_queue_create(
                label = "MetadataOutputQueue",
                attr = dispatch_queue_attr_make_with_qos_class(
                    attr = null,
                    qos_class = QOS_CLASS_USER_INITIATED,
                    relative_priority = 0
                )
            )
            metadataOutput.setMetadataObjectsDelegate(delegate, queue)
        }
    }

    awaitClose {
        dispatch_async(sessionQueue) {
            withConfiguration {
                metadataOutput.setMetadataObjectsDelegate(null, null)
                removeOutput(metadataOutput)
            }
            stableRef.dispose()
        }
    }
}.distinctUntilChanged()

@OptIn(ExperimentalForeignApi::class)
private class BarcodeDelegate(
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
