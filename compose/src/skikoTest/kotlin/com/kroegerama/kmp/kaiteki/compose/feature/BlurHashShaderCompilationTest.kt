package com.kroegerama.kmp.kaiteki.compose.feature

import com.kroegerama.kmp.kaiteki.feature.BlurHash
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class BlurHashShaderCompilationTest {

    @Test
    fun allComponentSizesCompileSuccessfully() {
        for (x in 1..9) {
            for (y in 1..9) {
                val source = buildBlurHashShaderSource(x, y)
                RuntimeEffect.makeForShader(source)
            }
        }
    }

    @Test
    fun shaderExecutesWithUniforms() {
        val decoded = BlurHash.decode("LGF5?xYk^6#M@-5c,1J5@[or[Q6.")!!
        val source = buildBlurHashShaderSource(decoded.componentsX, decoded.componentsY)
        val effect = RuntimeEffect.makeForShader(source)
        val shader = RuntimeShaderBuilder(effect).apply {
            uniform("resolution", 100f, 100f)
            uniform("factors", decoded.factors)
        }.makeShader()
        assertNotNull(shader)
    }
}
