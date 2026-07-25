package com.kroegerama.kmp.kaiteki.compose.graphics

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asSkiaPath
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathUtils

public actual fun Path.strokeToFill(
    strokeWidth: Float,
    miter: Float,
    cap: StrokeCap,
    join: StrokeJoin
): Path {
    val paint = Paint().apply {
        mode = PaintMode.STROKE
        this.strokeWidth = strokeWidth
        strokeMiter = miter
        strokeCap = when (cap) {
            StrokeCap.Round -> PaintStrokeCap.ROUND
            StrokeCap.Square -> PaintStrokeCap.SQUARE
            else -> PaintStrokeCap.BUTT
        }
        strokeJoin = when (join) {
            StrokeJoin.Round -> PaintStrokeJoin.ROUND
            StrokeJoin.Bevel -> PaintStrokeJoin.BEVEL
            else -> PaintStrokeJoin.MITER
        }
    }
    return try {
        PathUtils.fillPathWithPaint(asSkiaPath(), paint).asComposePath()
    } finally {
        paint.close()
    }
}
