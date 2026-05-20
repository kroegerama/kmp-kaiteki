package com.kroegerama.kmp.kaiteki.camera.compose

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.controller.CameraController

@ExperimentalKaitekiCameraApi
@Composable
public actual fun CameraView(
    controller: CameraController,
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

    Box(
        modifier = modifier
    ) {
        surfaceRequest?.let { request ->
            val coordinateTransformer = remember { MutableCoordinateTransformer() }
            val transformableState = rememberTransformableState(
                onTransformation = { _, zoomChange, _, _ ->
                    controller.zoomRatio *= zoomChange
                }
            )
            CameraXViewfinder(
                surfaceRequest = request,
                coordinateTransformer = coordinateTransformer,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .transformable(
                        state = transformableState
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                controller.toggleTorch()
                            },
                            onDoubleTap = {
                                val current = controller.zoomRatio
                                controller.zoomRatio = if (current <= 1.5f) 2f else 1f
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
}
