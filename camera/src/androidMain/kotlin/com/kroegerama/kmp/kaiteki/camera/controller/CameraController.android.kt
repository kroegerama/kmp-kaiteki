package com.kroegerama.kmp.kaiteki.camera.controller

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.analyzer.bindBarcodeAnalyzerFlow
import com.kroegerama.kmp.kaiteki.camera.analyzer.bindTextAnalyzerFlow
import com.kroegerama.kmp.kaiteki.camera.extensions.CameraControllerExtension
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

@ExperimentalKaitekiCameraApi
@Immutable
public actual class CameraController internal constructor(
    private val context: Context,
) {
    internal val executor = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private val _surfaceRequests = MutableStateFlow<SurfaceRequest?>(null)
    internal val surfaceRequests = _surfaceRequests.asStateFlow()
    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null
    private var camera: Camera? = null

    private val previewUseCase = Preview.Builder()
        .build()

    internal val analysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .build()

    public actual var zoomRatio: Float
        get() = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
        set(value) {
            val range = camera?.cameraInfo?.zoomState?.value?.run {
                minZoomRatio..maxZoomRatio
            }
            if (range != null && value !in range) return
            camera?.cameraControl?.setZoomRatio(value)
        }

    public actual var torchEnabled: Boolean
        get() = camera?.cameraInfo?.torchState?.value?.let {
            it == TorchState.ON
        } ?: false
        set(value) {
            camera?.cameraControl?.enableTorch(value)
        }

    public actual val torchAvailable: Boolean
        get() = camera?.cameraInfo?.hasFlashUnit() == true

    init {
        previewUseCase.setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequests.value = newSurfaceRequest
            surfaceMeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                newSurfaceRequest.resolution.width.toFloat(),
                newSurfaceRequest.resolution.height.toFloat()
            )
        }
    }

    internal suspend fun bindCamera(
        lifecycleOwner: LifecycleOwner
    ) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(context)
        processCameraProvider.unbindAll()
        cameraProvider = processCameraProvider

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(previewUseCase)
            .addUseCase(analysisUseCase)
            .build()

        camera = processCameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            useCaseGroup
        )
    }

    public actual fun toggleTorch() {
        torchEnabled = !torchEnabled
    }

    public actual fun focus(coords: Offset) {
        val point = surfaceMeteringPointFactory?.createPoint(coords.x, coords.y) ?: return
        val meteringAction = FocusMeteringAction.Builder(point).build()
        camera?.cameraControl?.startFocusAndMetering(meteringAction)
    }

    internal actual fun clear() {
        cameraProvider?.unbindAll()
        executor.shutdown()
    }
}

@ExperimentalKaitekiCameraApi
@Composable
public actual fun rememberCameraController(): CameraController {
    val context = LocalContext.current
    return remember(context) {
        CameraController(context)
    }
}

@ExperimentalKaitekiCameraApi
@Composable
public actual fun rememberBarcodeExtension(
    cameraController: CameraController,
    barcodeFormats: List<BarcodeFormat>
): CameraControllerExtension.BarcodeExtension {
    return remember(cameraController, barcodeFormats) {
        CameraControllerExtension.BarcodeExtension(
            formats = barcodeFormats,
            barcodeResults = cameraController.analysisUseCase.bindBarcodeAnalyzerFlow(
                zoomCallback = { cameraController.zoomRatio = it; true },
                formats = barcodeFormats,
                executor = cameraController.executor
            )
        )
    }
}

@ExperimentalKaitekiCameraApi
@Composable
public actual fun rememberOcrExtension(
    cameraController: CameraController
): CameraControllerExtension.OcrExtension {
    return remember(cameraController) {
        CameraControllerExtension.OcrExtension(
            ocrResults = cameraController.analysisUseCase.bindTextAnalyzerFlow(
                executor = cameraController.executor
            )
        )
    }
}
