package com.kroegerama.kmp.kaiteki.camera.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleOwner
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.delegate.BarcodeDelegate
import com.kroegerama.kmp.kaiteki.camera.delegate.TextDelegate
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import com.kroegerama.kmp.kaiteki.camera.withConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureExposureModeAutoExpose
import platform.AVFoundation.AVCaptureExposureModeContinuousAutoExposure
import platform.AVFoundation.AVCaptureFocusModeAutoFocus
import platform.AVFoundation.AVCaptureFocusModeContinuousAutoFocus
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset1920x1080
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoStabilizationModeStandard
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.exposureMode
import platform.AVFoundation.exposurePointOfInterest
import platform.AVFoundation.focusMode
import platform.AVFoundation.focusPointOfInterest
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isExposureModeSupported
import platform.AVFoundation.isExposurePointOfInterestSupported
import platform.AVFoundation.isFocusModeSupported
import platform.AVFoundation.isFocusPointOfInterestSupported
import platform.AVFoundation.torchMode
import platform.AVFoundation.videoMinZoomFactorForCinematicVideo
import platform.AVFoundation.videoZoomFactor
import platform.CoreGraphics.CGPointMake
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_attr_make_with_qos_class
import platform.darwin.dispatch_queue_create
import platform.posix.QOS_CLASS_USER_INITIATED

@ExperimentalKaitekiCameraApi
@OptIn(ExperimentalForeignApi::class)
internal actual class PlatformCameraController : CameraController {

    internal val session = AVCaptureSession()
    private var captureDevice: AVCaptureDevice? = null

    @OptIn(ExperimentalForeignApi::class)
    internal val sessionQueue = dispatch_queue_create(
        "CameraSessionQueue",
        dispatch_queue_attr_make_with_qos_class(null, QOS_CLASS_USER_INITIATED, 0)
    )

    actual override val zoomRatio: Float
        get() = captureDevice?.videoZoomFactor?.toFloat() ?: 1f

    actual override fun setZoomRatio(value: Float): Boolean {
        captureDevice?.withConfiguration {
            activeFormat.videoMinZoomFactorForCinematicVideo
            val newZoom = value.toDouble().coerceIn(
                1.0, activeFormat.videoMaxZoomFactor
            )
            videoZoomFactor = newZoom
        }
        return true
    }

    actual override var torchEnabled: Boolean
        get() = captureDevice?.torchMode == AVCaptureTorchModeOn
        set(value) {
            captureDevice?.withConfiguration {
                if (hasTorch) {
                    torchMode = if (value) {
                        AVCaptureTorchModeOn
                    } else {
                        AVCaptureTorchModeOff
                    }
                }
            }
        }

    actual override val torchAvailable: Boolean
        get() = captureDevice?.hasTorch == true

    actual override suspend fun bindCamera(
        lifecycleOwner: LifecycleOwner
    ) {
        dispatch_async(sessionQueue) {
            val captureDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.also {
                captureDevice = it
            } ?: return@dispatch_async
            captureDevice.withConfiguration {
                if (isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
                    focusMode = AVCaptureFocusModeContinuousAutoFocus
                }
                if (isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) {
                    exposureMode = AVCaptureExposureModeContinuousAutoExposure
                }
            }

            val input = AVCaptureDeviceInput.deviceInputWithDevice(captureDevice, null) ?: return@dispatch_async
            session.withConfiguration {
                if (captureDevice.supportsAVCaptureSessionPreset(AVCaptureSessionPreset1920x1080)) {
                    session.sessionPreset = AVCaptureSessionPreset1920x1080
                } else {
                    session.sessionPreset = AVCaptureSessionPresetHigh
                }
                if (session.canAddInput(input)) {
                    session.addInput(input)
                }
            }
            session.startRunning()
        }
    }

    actual override fun toggleTorch() {
        torchEnabled = !torchEnabled
    }

    actual override fun focus(coords: Offset) {
        captureDevice?.withConfiguration {
            val point = CGPointMake(coords.x.toDouble(), coords.y.toDouble())
            if (isFocusPointOfInterestSupported()) {
                focusPointOfInterest = point
                if (isFocusModeSupported(AVCaptureFocusModeAutoFocus)) {
                    focusMode = AVCaptureFocusModeAutoFocus
                }
            }
            if (isExposurePointOfInterestSupported()) {
                exposurePointOfInterest = point
                if (isExposureModeSupported(AVCaptureExposureModeAutoExpose)) {
                    exposureMode = AVCaptureExposureModeAutoExpose
                }
            }
        }
    }

    actual override fun clear() {
        dispatch_async(sessionQueue) {
            session.stopRunning()
        }
    }

    actual override fun bindBarcodeAnalyzerFlow(vararg formats: BarcodeFormat): Flow<BarcodeResult> = callbackFlow {
        val metadataOutput = AVCaptureMetadataOutput()
        val delegate = BarcodeDelegate(this)
        val stableRef = StableRef.create(delegate)

        dispatch_async(sessionQueue) {
            session.withConfiguration {
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
                session.withConfiguration {
                    metadataOutput.setMetadataObjectsDelegate(null, null)
                    removeOutput(metadataOutput)
                }
                stableRef.dispose()
            }
        }
    }.distinctUntilChanged()

    actual override fun bindTextAnalyzerFlow(minConfidence: Float): Flow<OCRResult> = callbackFlow<OCRResult> {
        val delegate = TextDelegate(
            producer = this,
            minConfidence = minConfidence
        )
        val stableRef = StableRef.create(delegate)

        val sessionQueue = dispatch_queue_create(label = "camera.session.queue", null)
        val videoDataOutput = AVCaptureVideoDataOutput()

        videoDataOutput.apply {
            alwaysDiscardsLateVideoFrames = true
            videoSettings = mapOf(
                kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA
            )
            setSampleBufferDelegate(delegate, sessionQueue)
        }

        dispatch_async(sessionQueue) {
            session.withConfiguration {
                if (canAddOutput(videoDataOutput)) {
                    addOutput(videoDataOutput)

                    videoDataOutput.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
                        connection.preferredVideoStabilizationMode = AVCaptureVideoStabilizationModeStandard
                        connection.enabled = true
                    }
                }
            }
        }

        awaitClose {
            dispatch_async(sessionQueue) {
                session.withConfiguration {
                    videoDataOutput.setSampleBufferDelegate(null, null)
                    removeOutput(videoDataOutput)
                }
                stableRef.dispose()
            }
        }
    }.distinctUntilChanged()
}

@ExperimentalKaitekiCameraApi
@Composable
internal actual fun rememberPlatformCameraController(): PlatformCameraController {
    return remember {
        PlatformCameraController()
    }
}
