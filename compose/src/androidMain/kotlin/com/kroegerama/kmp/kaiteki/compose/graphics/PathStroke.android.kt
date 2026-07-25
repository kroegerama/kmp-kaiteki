package com.kroegerama.kmp.kaiteki.compose.graphics

import android.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import android.graphics.Path as AndroidPath

public actual fun Path.strokeToFill(
    strokeWidth: Float,
    miter: Float,
    cap: StrokeCap,
    join: StrokeJoin
): Path {
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeMiter = miter
        strokeCap = when (cap) {
            StrokeCap.Round -> Paint.Cap.ROUND
            StrokeCap.Square -> Paint.Cap.SQUARE
            else -> Paint.Cap.BUTT
        }
        strokeJoin = when (join) {
            StrokeJoin.Round -> Paint.Join.ROUND
            StrokeJoin.Bevel -> Paint.Join.BEVEL
            else -> Paint.Join.MITER
        }
    }
    return AndroidPath().also { paint.getFillPath(asAndroidPath(), it) }.asComposePath()
}
