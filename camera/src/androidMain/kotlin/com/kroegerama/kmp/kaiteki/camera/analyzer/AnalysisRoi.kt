package com.kroegerama.kmp.kaiteki.camera.analyzer

import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import com.kroegerama.kmp.kaiteki.camera.model.rotateNormalized

/**
 * Maps a viewport-normalized [roi] into the upright, frame-normalized space of result
 * bounding boxes, or `null` if the mapping is unknown. Scales through
 * [ImageProxy.getCropRect] because ML Kit sees the full buffer and ignores the crop rect.
 */
internal fun uprightAnalysisRoi(roi: Rect?, imageProxy: ImageProxy): Rect? {
    if (roi == null) return null
    val crop = imageProxy.cropRect
    val width = imageProxy.width
    val height = imageProxy.height
    if (width <= 0 || height <= 0 || crop.isEmpty) return null
    val normalized = Rect(
        (crop.left + roi.left * crop.width()) / width,
        (crop.top + roi.top * crop.height()) / height,
        (crop.left + roi.right * crop.width()) / width,
        (crop.top + roi.bottom * crop.height()) / height,
    )
    return normalized.rotateNormalized(imageProxy.imageInfo.rotationDegrees)
}
