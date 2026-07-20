package com.kroegerama.kmp.kaiteki.camera.controller

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.LifecycleOwner
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeFormat
import com.kroegerama.kmp.kaiteki.camera.model.BarcodeResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal const val DEFAULT_MIN_OCR_CONFIDENCE: Float = 0.5f

/**
 * Controls the camera shown by [CameraView][com.kroegerama.kmp.kaiteki.camera.compose.CameraView].
 *
 * Obtain instances via [rememberCameraController]. Not intended to be implemented by library consumers.
 */
@ExperimentalKaitekiCameraApi
public interface CameraController {
    public suspend fun bindCamera(lifecycleOwner: LifecycleOwner)

    public fun clear()

    public val zoomRatio: Float
    public var torchEnabled: Boolean
    public val torchAvailable: Boolean

    public fun setZoomRatio(value: Float): Boolean
    public fun toggleTorch()
    public fun focus(coords: Offset)

    public fun bindBarcodeAnalyzerFlow(vararg formats: BarcodeFormat): Flow<BarcodeResult>
    public fun bindTextAnalyzerFlow(minConfidence: Float = DEFAULT_MIN_OCR_CONFIDENCE): Flow<OCRResult>
}

/** Non-functional controller returned in inspection mode (IDE previews). */
@ExperimentalKaitekiCameraApi
internal object DummyCameraController : CameraController {
    override suspend fun bindCamera(lifecycleOwner: LifecycleOwner) {}
    override fun clear() {}

    override val zoomRatio: Float = 1f
    override var torchEnabled: Boolean = false
    override val torchAvailable: Boolean = false

    override fun setZoomRatio(value: Float): Boolean = false
    override fun toggleTorch() {}
    override fun focus(coords: Offset) {}

    override fun bindBarcodeAnalyzerFlow(vararg formats: BarcodeFormat): Flow<BarcodeResult> = emptyFlow()
    override fun bindTextAnalyzerFlow(minConfidence: Float): Flow<OCRResult> = emptyFlow()
}

@ExperimentalKaitekiCameraApi
internal expect class PlatformCameraController : CameraController {
    override suspend fun bindCamera(lifecycleOwner: LifecycleOwner)
    override fun clear()

    override val zoomRatio: Float
    override var torchEnabled: Boolean
    override val torchAvailable: Boolean

    override fun setZoomRatio(value: Float): Boolean
    override fun toggleTorch()
    override fun focus(coords: Offset)

    override fun bindBarcodeAnalyzerFlow(vararg formats: BarcodeFormat): Flow<BarcodeResult>
    override fun bindTextAnalyzerFlow(minConfidence: Float): Flow<OCRResult>
}

/**
 * Creates and remembers a [CameraController].
 *
 * In [inspection mode][LocalInspectionMode] (e.g. IDE previews) a non-functional
 * controller is returned that never accesses any camera APIs.
 */
@ExperimentalKaitekiCameraApi
@Composable
public fun rememberCameraController(): CameraController {
    if (LocalInspectionMode.current) {
        return DummyCameraController
    }
    return rememberPlatformCameraController()
}

@ExperimentalKaitekiCameraApi
@Composable
internal expect fun rememberPlatformCameraController(): PlatformCameraController
