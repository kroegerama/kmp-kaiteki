package com.kroegerama.kmp.kaiteki.compose.feature

import kotlin.test.Test
import kotlin.test.assertContains

class BlurHashShaderSourceTest {

    @Test
    fun shaderSourceContainsExpectedStructure() {
        val source = buildBlurHashShaderSource(4, 3)
        assertContains(source, "uniform float2 resolution;")
        assertContains(source, "uniform half3 factors[12];")
        assertContains(source, "half4 main(float2 fragCoord)")
    }

    @Test
    fun shaderSourceGeneratesCorrectCosineTerms() {
        val source = buildBlurHashShaderSource(3, 2)
        assertContains(source, "half x0 = 1.0;")
        assertContains(source, "half x1 = cos(PI * uv.x);")
        assertContains(source, "half x2 = cos(2.0 * PI * uv.x);")
        assertContains(source, "half y0 = 1.0;")
        assertContains(source, "half y1 = cos(PI * uv.y);")
    }

    @Test
    fun shaderSourceAllComponentSizesAreValid() {
        for (x in 1..9) {
            for (y in 1..9) {
                val source = buildBlurHashShaderSource(x, y)
                assertContains(source, "factors[${x * y}]", message = "Missing factors for ${x}x${y}")
                assertContains(source, "half4 main(float2 fragCoord)", message = "Missing main for ${x}x${y}")
                assertContains(source, "linearToSrgb(color)", message = "Missing linearToSrgb for ${x}x${y}")
            }
        }
    }
}
