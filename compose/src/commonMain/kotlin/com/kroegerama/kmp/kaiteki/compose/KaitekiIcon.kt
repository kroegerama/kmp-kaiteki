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
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 0.15f,
                strokeAlpha = 0.15f
            ) {
                moveTo(4.072f, 23.74f)
                verticalLineTo(0.38f)
                horizontalLineToRelative(4.8f)
                verticalLineToRelative(12.416f)
                horizontalLineToRelative(2.432f)
                lineToRelative(3.681f, -6.656f)
                horizontalLineToRelative(5.279f)
                lineToRelative(-4.896f, 8.608f)
                lineToRelative(4.991f, 8.992f)
                horizontalLineToRelative(-5.375f)
                lineToRelative(-3.712f, -6.912f)
                horizontalLineToRelative(-2.4f)
                verticalLineToRelative(6.912f)
                horizontalLineToRelative(-4.8f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(2.389f, 10.592f)
                verticalLineToRelative(-6.57f)
                horizontalLineToRelative(1.35f)
                verticalLineToRelative(3.492f)
                horizontalLineToRelative(0.684f)
                lineToRelative(1.035f, -1.872f)
                horizontalLineToRelative(1.485f)
                lineToRelative(-1.377f, 2.421f)
                lineToRelative(1.404f, 2.529f)
                horizontalLineToRelative(-1.512f)
                lineToRelative(-1.044f, -1.944f)
                horizontalLineToRelative(-0.675f)
                verticalLineToRelative(1.944f)
                horizontalLineToRelative(-1.35f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.877f, 10.682f)
                curveToRelative(-0.492f, 0f, -0.881f, -0.142f, -1.169f, -0.427f)
                reflectiveCurveToRelative(-0.432f, -0.659f, -0.432f, -1.121f)
                curveToRelative(0f, -0.336f, 0.08f, -0.624f, 0.238f, -0.864f)
                curveToRelative(0.159f, -0.24f, 0.39f, -0.425f, 0.693f, -0.554f)
                curveToRelative(0.303f, -0.129f, 0.667f, -0.193f, 1.094f, -0.193f)
                horizontalLineToRelative(0.972f)
                verticalLineToRelative(-0.315f)
                curveToRelative(0f, -0.186f, -0.064f, -0.332f, -0.193f, -0.437f)
                reflectiveCurveToRelative(-0.313f, -0.158f, -0.554f, -0.158f)
                curveToRelative(-0.229f, 0f, -0.409f, 0.045f, -0.545f, 0.135f)
                curveToRelative(-0.135f, 0.09f, -0.208f, 0.222f, -0.221f, 0.396f)
                horizontalLineToRelative(-1.277f)
                curveToRelative(0.018f, -0.486f, 0.21f, -0.873f, 0.576f, -1.161f)
                curveToRelative(0.366f, -0.288f, 0.86f, -0.432f, 1.484f, -0.432f)
                curveToRelative(0.654f, 0f, 1.164f, 0.15f, 1.53f, 0.45f)
                curveToRelative(0.366f, 0.3f, 0.549f, 0.72f, 0.549f, 1.26f)
                verticalLineToRelative(3.33f)
                horizontalLineToRelative(-1.305f)
                verticalLineToRelative(-0.981f)
                horizontalLineToRelative(-0.216f)
                lineToRelative(0.243f, -0.144f)
                curveToRelative(0f, 0.246f, -0.061f, 0.46f, -0.181f, 0.644f)
                reflectiveCurveToRelative(-0.289f, 0.324f, -0.508f, 0.423f)
                curveToRelative(-0.22f, 0.099f, -0.479f, 0.148f, -0.779f, 0.148f)
                close()
                moveTo(9.355f, 9.602f)
                curveToRelative(0.282f, 0f, 0.505f, -0.07f, 0.671f, -0.211f)
                curveToRelative(0.164f, -0.141f, 0.247f, -0.325f, 0.247f, -0.553f)
                verticalLineToRelative(-0.504f)
                horizontalLineToRelative(-0.945f)
                curveToRelative(-0.24f, 0f, -0.424f, 0.058f, -0.554f, 0.175f)
                curveToRelative(-0.129f, 0.117f, -0.193f, 0.269f, -0.193f, 0.455f)
                reflectiveCurveToRelative(0.068f, 0.339f, 0.203f, 0.459f)
                reflectiveCurveToRelative(0.325f, 0.18f, 0.571f, 0.18f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.514f, 10.592f)
                verticalLineToRelative(-1.215f)
                horizontalLineToRelative(1.638f)
                verticalLineToRelative(-2.52f)
                horizontalLineToRelative(-1.458f)
                verticalLineToRelative(-1.215f)
                horizontalLineToRelative(2.809f)
                verticalLineToRelative(3.735f)
                horizontalLineToRelative(1.422f)
                verticalLineToRelative(1.215f)
                horizontalLineToRelative(-4.411f)
                close()
                moveTo(14.765f, 4.859f)
                curveToRelative(-0.246f, 0f, -0.442f, -0.063f, -0.59f, -0.189f)
                curveToRelative(-0.146f, -0.126f, -0.22f, -0.297f, -0.22f, -0.513f)
                reflectiveCurveToRelative(0.073f, -0.387f, 0.22f, -0.513f)
                curveToRelative(0.147f, -0.126f, 0.344f, -0.189f, 0.59f, -0.189f)
                reflectiveCurveToRelative(0.442f, 0.063f, 0.59f, 0.189f)
                curveToRelative(0.146f, 0.126f, 0.221f, 0.297f, 0.221f, 0.513f)
                reflectiveCurveToRelative(-0.074f, 0.387f, -0.221f, 0.513f)
                curveToRelative(-0.147f, 0.126f, -0.344f, 0.189f, -0.59f, 0.189f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.945f, 20.593f)
                curveToRelative(-0.504f, 0f, -0.9f, -0.144f, -1.188f, -0.432f)
                curveToRelative(-0.288f, -0.288f, -0.432f, -0.685f, -0.432f, -1.188f)
                verticalLineToRelative(-2.115f)
                horizontalLineToRelative(-1.332f)
                verticalLineToRelative(-1.215f)
                horizontalLineToRelative(1.332f)
                verticalLineToRelative(-1.395f)
                horizontalLineToRelative(1.35f)
                verticalLineToRelative(1.395f)
                horizontalLineToRelative(1.935f)
                verticalLineToRelative(1.215f)
                horizontalLineToRelative(-1.935f)
                verticalLineToRelative(2.07f)
                curveToRelative(0f, 0.132f, 0.035f, 0.24f, 0.104f, 0.324f)
                reflectiveCurveToRelative(0.169f, 0.126f, 0.302f, 0.126f)
                horizontalLineToRelative(1.485f)
                verticalLineToRelative(1.215f)
                horizontalLineToRelative(-1.62f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.526f, 20.683f)
                curveToRelative(-0.438f, 0f, -0.82f, -0.082f, -1.147f, -0.247f)
                reflectiveCurveToRelative(-0.579f, -0.396f, -0.756f, -0.693f)
                curveToRelative(-0.177f, -0.297f, -0.266f, -0.644f, -0.266f, -1.039f)
                verticalLineToRelative(-1.17f)
                curveToRelative(0f, -0.396f, 0.088f, -0.743f, 0.266f, -1.04f)
                curveToRelative(0.177f, -0.297f, 0.429f, -0.527f, 0.756f, -0.693f)
                curveToRelative(0.327f, -0.164f, 0.709f, -0.247f, 1.147f, -0.247f)
                reflectiveCurveToRelative(0.817f, 0.083f, 1.139f, 0.247f)
                curveToRelative(0.32f, 0.166f, 0.569f, 0.396f, 0.747f, 0.693f)
                curveToRelative(0.177f, 0.297f, 0.266f, 0.644f, 0.266f, 1.04f)
                verticalLineToRelative(0.918f)
                horizontalLineToRelative(-3.033f)
                verticalLineToRelative(0.252f)
                curveToRelative(0f, 0.312f, 0.073f, 0.54f, 0.221f, 0.684f)
                curveToRelative(0.146f, 0.145f, 0.367f, 0.216f, 0.661f, 0.216f)
                curveToRelative(0.192f, 0f, 0.36f, -0.029f, 0.504f, -0.09f)
                curveToRelative(0.145f, -0.06f, 0.229f, -0.149f, 0.252f, -0.27f)
                horizontalLineToRelative(1.323f)
                curveToRelative(-0.096f, 0.432f, -0.333f, 0.779f, -0.711f, 1.044f)
                curveToRelative(-0.378f, 0.264f, -0.834f, 0.396f, -1.368f, 0.396f)
                close()
                moveTo(10.39f, 17.74f)
                verticalLineToRelative(-0.226f)
                curveToRelative(0f, -0.306f, -0.071f, -0.543f, -0.212f, -0.711f)
                reflectiveCurveToRelative(-0.358f, -0.252f, -0.652f, -0.252f)
                reflectiveCurveToRelative(-0.515f, 0.087f, -0.661f, 0.261f)
                curveToRelative(-0.147f, 0.175f, -0.221f, 0.414f, -0.221f, 0.721f)
                verticalLineToRelative(0.116f)
                lineToRelative(1.836f, -0.018f)
                lineToRelative(-0.09f, 0.108f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.523f, 20.593f)
                verticalLineToRelative(-6.57f)
                horizontalLineToRelative(1.351f)
                verticalLineToRelative(3.492f)
                horizontalLineToRelative(0.684f)
                lineToRelative(1.035f, -1.872f)
                horizontalLineToRelative(1.485f)
                lineToRelative(-1.377f, 2.421f)
                lineToRelative(1.403f, 2.529f)
                horizontalLineToRelative(-1.512f)
                lineToRelative(-1.044f, -1.944f)
                horizontalLineToRelative(-0.675f)
                verticalLineToRelative(1.944f)
                horizontalLineToRelative(-1.351f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.581f, 20.593f)
                verticalLineToRelative(-1.215f)
                horizontalLineToRelative(1.638f)
                verticalLineToRelative(-2.521f)
                horizontalLineToRelative(-1.458f)
                verticalLineToRelative(-1.215f)
                horizontalLineToRelative(2.809f)
                verticalLineToRelative(3.735f)
                horizontalLineToRelative(1.422f)
                verticalLineToRelative(1.215f)
                horizontalLineToRelative(-4.41f)
                close()
                moveTo(19.831f, 14.859f)
                curveToRelative(-0.246f, 0f, -0.442f, -0.062f, -0.59f, -0.188f)
                curveToRelative(-0.146f, -0.126f, -0.22f, -0.297f, -0.22f, -0.513f)
                reflectiveCurveToRelative(0.073f, -0.388f, 0.22f, -0.514f)
                curveToRelative(0.147f, -0.126f, 0.344f, -0.188f, 0.59f, -0.188f)
                reflectiveCurveToRelative(0.442f, 0.062f, 0.59f, 0.188f)
                curveToRelative(0.146f, 0.126f, 0.221f, 0.297f, 0.221f, 0.514f)
                reflectiveCurveToRelative(-0.074f, 0.387f, -0.221f, 0.513f)
                curveToRelative(-0.147f, 0.126f, -0.344f, 0.188f, -0.59f, 0.188f)
                close()
            }
        }.build()

        return _KaitekiIcon!!
    }

@Suppress("ObjectPropertyName")
private var _KaitekiIcon: ImageVector? = null
