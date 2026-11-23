package com.kroegerama.kmp.kaiteki.compose.feature

import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi
import com.kroegerama.kmp.kaiteki.feature.ThumbHash
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalKaitekiApi::class)
class ThumbHashPixelsTest {

    @Test
    fun referenceVectorsAreComplete() {
        ThumbHashReferenceVectors.forEach { vector ->
            assertEquals(25 * 8, vector.samples.length, vector.base64)
        }
    }

    @Test
    fun cpuDecoderMatchesReferencePixels() {
        ThumbHashReferenceVectors.forEach { vector ->
            val thumbHash = assertNotNull(ThumbHash.decode(vector.base64))
            assertEquals(vector.width, thumbHash.width, vector.base64)
            assertEquals(vector.height, thumbHash.height, vector.base64)
            val pixels = thumbHash.toArgbPixels()
            assertEquals(vector.width * vector.height, pixels.size)
            vector.forEachSample { x, y, expected ->
                val argb = pixels[y * vector.width + x]
                val actual = intArrayOf(argb shr 16 and 255, argb shr 8 and 255, argb and 255, argb ushr 24)
                for (channel in 0..3) {
                    assertTrue(
                        abs(expected[channel] - actual[channel]) <= 1,
                        "${vector.base64} at ($x, $y): expected ${expected.toList()}, got ${actual.toList()}"
                    )
                }
            }
        }
    }
}
