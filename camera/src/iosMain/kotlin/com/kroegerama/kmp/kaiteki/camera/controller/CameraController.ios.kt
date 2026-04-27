package com.kroegerama.kmp.kaiteki.camera.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import com.kroegerama.kmp.kaiteki.camera.delegate.bindBarcodeDelegateFlow
import com.kroegerama.kmp.kaiteki.camera.delegate.bindTextDelegateFlow
import com.kroegerama.kmp.kaiteki.camera.extensions.CameraControllerExtension
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.withConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureExposureModeAutoExpose
import platform.AVFoundation.AVCaptureExposureModeContinuousAutoExposure
import platform.AVFoundation.AVCaptureFocusModeAutoFocus
import platform.AVFoundation.AVCaptureFocusModeContinuousAutoFocus
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset1920x1080
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
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
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_attr_make_with_qos_class
import platform.darwin.dispatch_queue_create
import platform.posix.QOS_CLASS_USER_INITIATED

@Immutable
@OptIn(ExperimentalForeignApi::class, ExperimentalForeignApi::class)
public actual class CameraController {
    internal val session = AVCaptureSession()
    private var captureDevice: AVCaptureDevice? = null

    @OptIn(ExperimentalForeignApi::class)
    internal val sessionQueue = dispatch_queue_create(
        "CameraSessionQueue",
        dispatch_queue_attr_make_with_qos_class(null, QOS_CLASS_USER_INITIATED, 0)
    )

    public actual var zoomRatio: Float
        get() = captureDevice?.videoZoomFactor?.toFloat() ?: 1f
        set(value) {
            captureDevice?.withConfiguration {
                activeFormat.videoMinZoomFactorForCinematicVideo
                val newZoom = value.toDouble().coerceIn(
                    1.0, activeFormat.videoMaxZoomFactor
                )
                videoZoomFactor = newZoom
            }
        }

    public actual var torchEnabled: Boolean
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

    public actual val torchAvailable: Boolean
        get() = captureDevice?.hasTorch == true

    internal fun bindCamera() {
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

    public actual fun toggleTorch() {
        torchEnabled = !torchEnabled
    }

    public actual fun focus(coords: Offset) {
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

    internal actual fun clear() {
        dispatch_async(sessionQueue) {
            session.stopRunning()
        }
    }
}

@Composable
public actual fun rememberCameraController(
): CameraController {
    return remember {
        CameraController()
    }
}

@Composable
public actual fun rememberBarcodeExtension(
    cameraController: CameraController,
    barcodeFormats: List<BarcodeFormat>
): CameraControllerExtension.BarcodeExtension {
    return remember(cameraController, barcodeFormats) {
        CameraControllerExtension.BarcodeExtension(
            formats = barcodeFormats,
            barcodeResults = cameraController.session.bindBarcodeDelegateFlow(
                formats = barcodeFormats,
                sessionQueue = cameraController.sessionQueue
            )
        )
    }
}

@Composable
public actual fun rememberOcrExtension(
    cameraController: CameraController
): CameraControllerExtension.OcrExtension {
    return remember(cameraController) {
        CameraControllerExtension.OcrExtension(
            ocrResults = cameraController.session.bindTextDelegateFlow(
                sessionQueue = cameraController.sessionQueue
            )
        )
    }
}
