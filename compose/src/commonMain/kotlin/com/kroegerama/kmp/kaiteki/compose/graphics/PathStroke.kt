package com.kroegerama.kmp.kaiteki.compose.graphics

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** Converts this stroke centerline into its filled outline. */
public expect fun Path.strokeToFill(
    strokeWidth: Float,
    miter: Float = Stroke.DefaultMiter,
    cap: StrokeCap = StrokeCap.Butt,
    join: StrokeJoin = StrokeJoin.Bevel
): Path
