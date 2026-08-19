package com.kroegerama.kmp.kaiteki.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.controller.PlatformCameraController
import com.kroegerama.kmp.kaiteki.camera.model.AnalysisRegion
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.launch
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVCaptureVideoStabilizationModeCinematicExtended
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreGraphics.CGRectInset
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

@ExperimentalKaitekiCameraApi
@Composable
internal actual fun PlatformCameraView(
    controller: PlatformCameraController,
    modifier: Modifier,
    analysisRegion: AnalysisRegion
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
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
            // The analysis region only converts into output coordinates once the session runs.
            controller.onSessionStarted = { container.setNeedsLayout() }
            scope.launch { controller.bindCamera(lifecycleOwner) }
            container
        },
        update = { view ->
            view.analysisRegion = analysisRegion
        },
        onRelease = {
            controller.onSessionStarted = null
            controller.clear()
        },
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        ),
        modifier = modifier
    )
}

@ExperimentalKaitekiCameraApi
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class CameraPreviewUIView(
    private val controller: PlatformCameraController,
    private val previewLayer: AVCaptureVideoPreviewLayer,
) : UIView(frame = CGRectZero.readValue()) {

    private var lastPinchZoom: Double = 1.0

    var analysisRegion: AnalysisRegion = AnalysisRegion.FullFrame
        set(value) {
            if (field != value) {
                field = value
                setNeedsLayout()
            }
        }

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
        val rotationAngle = when (orientation) {
            UIInterfaceOrientationLandscapeLeft -> 180.0
            UIInterfaceOrientationLandscapeRight -> 0.0
            UIInterfaceOrientationPortraitUpsideDown -> 270.0
            else -> 90.0
        }
        previewLayer.connection?.videoRotationAngle = rotationAngle
        CATransaction.commit()
        updateAnalysisRoi(rotationAngle.toInt())
    }

    private fun updateAnalysisRoi(uprightRotationDegrees: Int) {
        val region = analysisRegion
        if (region !is AnalysisRegion.VisibleArea) {
            controller.updateAnalysisGeometry(null, null, uprightRotationDegrees)
            return
        }
        // A Dp equals a point on iOS.
        val inset = region.inset.value.toDouble()
        val (width, height) = bounds.useContents { size.width to size.height }
        if (width <= inset * 2 || height <= inset * 2) {
            controller.updateAnalysisGeometry(null, null, uprightRotationDegrees)
            return
        }
        val viewfinderRect = Rect(
            (inset / width).toFloat(),
            (inset / height).toFloat(),
            ((width - inset) / width).toFloat(),
            ((height - inset) / height).toFloat(),
        )
        // View coordinates to unrotated output space, honoring the aspect-fill crop;
        // degenerate until the session provides geometry, which scans the full frame.
        val outputRect = previewLayer.metadataOutputRectOfInterestForRect(
            CGRectInset(bounds, inset, inset)
        ).useContents {
            Rect(
                origin.x.toFloat(),
                origin.y.toFloat(),
                (origin.x + size.width).toFloat(),
                (origin.y + size.height).toFloat(),
            )
        }
        controller.updateAnalysisGeometry(outputRect.takeUnless { it.isEmpty }, viewfinderRect, uprightRotationDegrees)
    }

    @ObjCAction
    fun handleDoubleTap() {
        controller.setZoomRatio(if (controller.zoomRatio <= 1.5f) 2f else 1f)
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
                val ratio = (lastPinchZoom * recognizer.scale).toFloat()
                controller.setZoomRatio(ratio)
            }
        }
    }
}
