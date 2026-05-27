package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import com.kroegerama.kmp.kaiteki.feature.BlurHash
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun createBlurHashBrush(
    decoded: BlurHash,
    intrinsicSize: Size
): Brush {
    val source = buildBlurHashShaderSource(
        componentsX = decoded.componentsX,
        componentsY = decoded.componentsY
    )
    val runtimeEffect = RuntimeEffect.makeForShader(source)
    return BlurHashShaderBrush(
        runtimeEffect = runtimeEffect,
        blurHash = decoded,
        intrinsicSize = intrinsicSize
    )
}

internal class BlurHashShaderBrush(
    private val runtimeEffect: RuntimeEffect,
    private val blurHash: BlurHash,
    override val intrinsicSize: Size
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        return RuntimeShaderBuilder(runtimeEffect).apply {
            uniform("resolution", size.width, size.height)
            uniform("factors", blurHash.factors)
        }.makeShader().asComposeShader()
    }
}
