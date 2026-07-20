package com.kroegerama.kmp.kaiteki.camera.controller

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.analyzer.BarcodeAnalyzer
import com.kroegerama.kmp.kaiteki.camera.analyzer.TextAnalyzer
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executors

@ExperimentalKaitekiCameraApi
internal actual class PlatformCameraController(
    private val context: Context,
) : CameraController {

    private var cameraProvider: ProcessCameraProvider? = null

    private val _surfaceRequests = MutableStateFlow<SurfaceRequest?>(null)
    internal val surfaceRequests = _surfaceRequests.asStateFlow()

    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null
    private var camera: Camera? = null

    // Preview and analysis share the same aspect ratio, so analysis frames cover
    // the same field of view as the preview and relative coordinates line up.
    private val previewUseCase = Preview.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        )
        .build()

    internal val analysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    // Sizes are expressed in the sensor coordinate frame (landscape).
                    ResolutionStrategy(
                        Size(1920, 1080),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()
        )
        .build()

    actual override val zoomRatio: Float
        get() = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f

    actual override fun setZoomRatio(value: Float): Boolean {
        val range = camera?.cameraInfo?.zoomState?.value?.run {
            minZoomRatio..maxZoomRatio
        }
        if (range != null && value !in range) return false
        camera?.cameraControl?.setZoomRatio(value)
        return true
    }

    actual override var torchEnabled: Boolean
        get() = camera?.cameraInfo?.torchState?.value?.let {
            it == TorchState.ON
        } ?: false
        set(value) {
            camera?.cameraControl?.enableTorch(value)
        }

    actual override val torchAvailable: Boolean
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

    fun updateTargetRotation(rotation: Int) {
        previewUseCase.targetRotation = rotation
        analysisUseCase.targetRotation = rotation
    }

    actual override suspend fun bindCamera(
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

    actual override fun toggleTorch() {
        torchEnabled = !torchEnabled
    }

    actual override fun focus(coords: Offset) {
        val point = surfaceMeteringPointFactory?.createPoint(coords.x, coords.y) ?: return
        val meteringAction = FocusMeteringAction.Builder(point).build()
        camera?.cameraControl?.startFocusAndMetering(meteringAction)
    }

    actual override fun clear() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        camera = null
        _surfaceRequests.value = null
    }

    actual override fun bindBarcodeAnalyzerFlow(vararg formats: BarcodeFormat): Flow<BarcodeResult> = callbackFlow {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = BarcodeAnalyzer(
            producer = this,
            zoomCallback = ::setZoomRatio,
            formats = formats
        )
        analysisUseCase.setAnalyzer(executor, analyzer)
        awaitClose {
            analysisUseCase.clearAnalyzer()
            analyzer.close()
            executor.shutdown()
        }
    }

    actual override fun bindTextAnalyzerFlow(minConfidence: Float): Flow<OCRResult> = callbackFlow {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = TextAnalyzer(
            producer = this,
            minConfidence = minConfidence
        )
        analysisUseCase.setAnalyzer(executor, analyzer)
        awaitClose {
            analysisUseCase.clearAnalyzer()
            analyzer.close()
            executor.shutdown()
        }
    }
}

@ExperimentalKaitekiCameraApi
@Composable
internal actual fun rememberPlatformCameraController(): PlatformCameraController {
    val context = LocalContext.current
    return remember(context) {
        PlatformCameraController(context)
    }
}
