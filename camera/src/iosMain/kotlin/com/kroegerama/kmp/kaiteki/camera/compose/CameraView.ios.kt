package com.kroegerama.kmp.kaiteki.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.kroegerama.kmp.kaiteki.camera.controller.CameraController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVCaptureVideoStabilizationModeCinematicExtended
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIPinchGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.darwin.sel_registerName

@Composable
public actual fun CameraView(
    controller: CameraController,
    modifier: Modifier
) {
    UIKitView(
        factory = {
            val previewLayer = AVCaptureVideoPreviewLayer(
                session = controller.session
            ).apply {
                videoGravity = AVLayerVideoGravityResizeAspectFill
                connection?.run {
                    if (isVideoStabilizationSupported()) {
                        preferredVideoStabilizationMode = AVCaptureVideoStabilizationModeCinematicExtended
                    }
                }
            }

            val container = CameraPreviewUIView(controller, previewLayer)
            container.layer.addSublayer(previewLayer)
            controller.bindCamera()
            container
        },
        onRelease = {
            controller.clear()
        },
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class CameraPreviewUIView(
    private val controller: CameraController,
    private val previewLayer: AVCaptureVideoPreviewLayer,
) : UIView(frame = CGRectZero.readValue()) {

    private var lastPinchZoom: Double = 1.0

    init {
        val doubleTap = UITapGestureRecognizer(
            target = this,
            action = sel_registerName("handleDoubleTap")
        ).apply {
            numberOfTapsRequired = 2u
        }

        val singleTap = UITapGestureRecognizer(
            target = this,
            action = sel_registerName("handleSingleTap:")
        ).apply {
            requireGestureRecognizerToFail(doubleTap)
        }

        val pinch = UIPinchGestureRecognizer(
            target = this,
            action = sel_registerName("handlePinch:")
        )

        addGestureRecognizer(doubleTap)
        addGestureRecognizer(singleTap)
        addGestureRecognizer(pinch)

        val longPress = UILongPressGestureRecognizer(
            target = this,
            action = sel_registerName("handleLongPress:")
        )
        addGestureRecognizer(longPress)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        previewLayer.setFrame(bounds)
        val orientation = window?.windowScene?.interfaceOrientation
        previewLayer.connection?.videoRotationAngle = when (orientation) {
            UIInterfaceOrientationLandscapeLeft -> 180.0
            UIInterfaceOrientationLandscapeRight -> 0.0
            UIInterfaceOrientationPortraitUpsideDown -> 270.0
            else -> 90.0
        }
        CATransaction.commit()
    }

    @ObjCAction
    fun handleDoubleTap() {
        val current = controller.zoomRatio
        controller.zoomRatio = if (current <= 1.5f) 2f else 1f
    }

    @ObjCAction
    fun handleLongPress(recognizer: UILongPressGestureRecognizer) {
        if (recognizer.state == UIGestureRecognizerStateBegan) {
            controller.toggleTorch()
        }
    }

    @ObjCAction
    fun handleSingleTap(recognizer: UITapGestureRecognizer) {
        val location = recognizer.locationInView(this)
        val devicePoint = previewLayer.captureDevicePointOfInterestForPoint(location)
        devicePoint.useContents {
            controller.focus(Offset(x.toFloat(), y.toFloat()))
        }
    }

    @ObjCAction
    fun handlePinch(recognizer: UIPinchGestureRecognizer) {
        when (recognizer.state) {
            UIGestureRecognizerStateBegan -> {
                lastPinchZoom = controller.zoomRatio.toDouble()
            }

            UIGestureRecognizerStateChanged -> {
                controller.zoomRatio = (lastPinchZoom * recognizer.scale).toFloat()
            }
        }
    }
}
