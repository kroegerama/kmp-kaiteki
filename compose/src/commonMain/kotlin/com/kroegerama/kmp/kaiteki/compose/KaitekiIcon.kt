package com.kroegerama.kmp.kaiteki.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val KaitekiIcon: ImageVector
    get() {
        if (_KaitekiIcon != null) {
            return _KaitekiIcon!!
        }
        _KaitekiIcon = ImageVector.Builder(
            name = "KaitekiIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(1.866f, 16.38f)
                verticalLineTo(7.62f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(4.752f)
                horizontalLineToRelative(1.056f)
                lineToRelative(1.572f, -2.592f)
                horizontalLineToRelative(1.668f)
                lineToRelative(-1.956f, 3.192f)
                lineToRelative(2.028f, 3.408f)
                horizontalLineToRelative(-1.704f)
                lineToRelative(-1.608f, -2.736f)
                horizontalLineToRelative(-1.056f)
                verticalLineToRelative(2.736f)
                horizontalLineToRelative(-1.5f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.342f, 16.38f)
                curveToRelative(-0.616f, 0f, -1.106f, -0.178f, -1.47f, -0.534f)
                curveToRelative(-0.364f, -0.355f, -0.546f, -0.838f, -0.546f, -1.446f)
                verticalLineToRelative(-3.264f)
                horizontalLineToRelative(-1.824f)
                verticalLineToRelative(-1.356f)
                horizontalLineToRelative(1.824f)
                verticalLineToRelative(-1.86f)
                horizontalLineToRelative(1.512f)
                verticalLineToRelative(1.86f)
                horizontalLineToRelative(2.628f)
                verticalLineToRelative(1.356f)
                horizontalLineToRelative(-2.628f)
                verticalLineToRelative(3.228f)
                curveToRelative(0f, 0.192f, 0.054f, 0.351f, 0.162f, 0.475f)
                reflectiveCurveToRelative(0.258f, 0.186f, 0.45f, 0.186f)
                horizontalLineToRelative(1.956f)
                verticalLineToRelative(1.356f)
                horizontalLineToRelative(-2.064f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.267f, 16.38f)
                verticalLineTo(7.62f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(4.752f)
                horizontalLineToRelative(1.056f)
                lineToRelative(1.572f, -2.592f)
                horizontalLineToRelative(1.668f)
                lineToRelative(-1.956f, 3.192f)
                lineToRelative(2.027f, 3.408f)
                horizontalLineToRelative(-1.703f)
                lineToRelative(-1.608f, -2.736f)
                horizontalLineToRelative(-1.056f)
                verticalLineToRelative(2.736f)
                horizontalLineToRelative(-1.5f)
                close()
            }
            path(
                fillAlpha = 0.33f,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 0.33f,
                strokeLineWidth = 1f
            ) {
                moveTo(5f, 20f)
                lineTo(19f, 20f)
            }
            path(
                fillAlpha = 0.33f,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 0.33f,
                strokeLineWidth = 1f
            ) {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
            }
        }.build()

        return _KaitekiIcon!!
    }

@Suppress("ObjectPropertyName")
private var _KaitekiIcon: ImageVector? = null
