package com.kroegerama.kmp.kaiteki.compose.feature

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import com.kroegerama.kmp.kaiteki.feature.BlurHash

internal actual fun createBlurHashBrush(
    decoded: BlurHash,
    intrinsicSize: Size
): Brush {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return SolidColor(Color(decoded.averageColor))
    }
    val source = buildBlurHashShaderSource(
        componentsX = decoded.componentsX,
        componentsY = decoded.componentsY
    )
    return BlurHashShaderBrush(
        shaderSource = source,
        blurHash = decoded,
        intrinsicSize = intrinsicSize
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class BlurHashShaderBrush(
    private val shaderSource: String,
    private val blurHash: BlurHash,
    override val intrinsicSize: Size
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        return RuntimeShader(shaderSource).apply {
            setFloatUniform("factors", blurHash.factors)
            setFloatUniform("resolution", size.width, size.height)
        }
    }
}
