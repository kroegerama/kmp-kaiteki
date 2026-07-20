package com.kroegerama.kmp.kaiteki.camera.compose

import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.controller.PlatformCameraController

@ExperimentalKaitekiCameraApi
@Composable
internal actual fun PlatformCameraView(
    controller: PlatformCameraController,
    modifier: Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceRequest by controller.surfaceRequests.collectAsStateWithLifecycle()

    LaunchedEffect(lifecycleOwner) {
        controller.bindCamera(lifecycleOwner)
    }
    DisposableEffect(lifecycleOwner) {
        onDispose { controller.clear() }
    }
    RotationSync(controller)

    Crossfade(
        targetState = surfaceRequest,
        animationSpec = tween(700),
        modifier = modifier
    ) { request ->
        request ?: return@Crossfade

        val coordinateTransformer = remember { MutableCoordinateTransformer() }
        CameraXViewfinder(
            surfaceRequest = request,
            // EMBEDDED (TextureView) is rendered inside the view hierarchy, so Compose
            // clipping and transformations (e.g. predictive back) apply to the preview.
            implementationMode = ImplementationMode.EMBEDDED,
            coordinateTransformer = coordinateTransformer,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } > 1) {
                                val zoomChange = event.calculateZoom()
                                if (zoomChange != 1f) {
                                    controller.setZoomRatio(controller.zoomRatio * zoomChange)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            controller.toggleTorch()
                        },
                        onDoubleTap = {
                            controller.setZoomRatio(if (controller.zoomRatio <= 1.5f) 2f else 1f)
                        },
                        onTap = { offset ->
                            with(coordinateTransformer) {
                                controller.focus(offset.transform())
                            }
                        }
                    )
                }
        )
    }
}

@OptIn(ExperimentalKaitekiCameraApi::class)
@Composable
private fun RotationSync(
    cameraController: PlatformCameraController
) {
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(view) {
        fun apply() {
            val rotation = view.display?.rotation ?: return
            cameraController.updateTargetRotation(rotation)
        }
        apply()

        val dm = context.getSystemService<DisplayManager>() ?: return@DisposableEffect onDispose {}
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) {}
            override fun onDisplayRemoved(id: Int) {}
            override fun onDisplayChanged(id: Int) {
                if (id == view.display?.displayId) apply()
            }
        }
        dm.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { dm.unregisterDisplayListener(listener) }
    }
}
