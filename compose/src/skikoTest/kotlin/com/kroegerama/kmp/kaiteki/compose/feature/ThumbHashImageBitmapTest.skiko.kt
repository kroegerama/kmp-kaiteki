package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.v2.runComposeUiTest
import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi
import com.kroegerama.kmp.kaiteki.feature.ThumbHash
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalKaitekiApi::class)
class ThumbHashImageBitmapTest {

    @Test
    fun imageBitmapHoldsTheDecodedPixels() = runComposeUiTest {
        ThumbHashReferenceVectors.forEach { vector ->
            val thumbHash = assertNotNull(ThumbHash.decode(vector.base64))
            val expected = thumbHash.toArgbPixels()
            val image = thumbHash.toImageBitmap()
            assertEquals(thumbHash.width, image.width)
            assertEquals(thumbHash.height, image.height)
            val pixelMap = image.toPixelMap()
            for (y in 0..<image.height) {
                for (x in 0..<image.width) {
                    val argb = expected[y * image.width + x]
                    val expectedAlpha = (argb ushr 24) / 255f
                    val actual = pixelMap[x, y]
                    assertTrue(abs(expectedAlpha - actual.alpha) <= 1.5f / 255f, "${vector.base64} alpha at ($x, $y)")
                    listOf(
                        (argb shr 16 and 255) to actual.red,
                        (argb shr 8 and 255) to actual.green,
                        (argb and 255) to actual.blue
                    ).forEach { (component, actualComponent) ->
                        val expectedPremultiplied = component / 255f * expectedAlpha
                        val actualPremultiplied = actualComponent * actual.alpha
                        assertTrue(
                            abs(expectedPremultiplied - actualPremultiplied) <= 1.5f / 255f,
                            "${vector.base64} at ($x, $y): expected $component, got $actual"
                        )
                    }
                }
            }
        }
    }
}
