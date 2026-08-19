package com.kroegerama.kmp.kaiteki.camera.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi

/**
 * The part of the analysis frame whose results are reported by the analyzer flows.
 */
@ExperimentalKaitekiCameraApi
@Immutable
public sealed interface AnalysisRegion {

    /** The whole analysis frame is scanned. */
    public data object FullFrame : AnalysisRegion

    /**
     * Only results fully contained in the visible viewfinder area, shrunk on all sides by [inset],
     * are reported. Behaves like [FullFrame] until the viewfinder geometry is known, when
     * [inset] is so large that the region collapses to an empty area, or on Android devices
     * whose preview buffer cannot match the 16:9 viewport aspect ratio.
     *
     * On Android, a barcode without a reported bounding box is treated as covering the whole
     * frame and is therefore never contained in a shrunk region.
     */
    public data class VisibleArea(val inset: Dp = 0.dp) : AnalysisRegion
}
