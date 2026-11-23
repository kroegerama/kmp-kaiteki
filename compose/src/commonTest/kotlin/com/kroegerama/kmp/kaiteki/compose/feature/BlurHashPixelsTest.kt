package com.kroegerama.kmp.kaiteki.compose.feature

import com.kroegerama.kmp.kaiteki.feature.BlurHash
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlurHashPixelsTest {

    @Test
    fun referenceVectorsAreComplete() {
        BlurHashReferenceVectors.forEach { vector ->
            assertEquals(25 * 8, vector.samples.length, vector.blurHash)
        }
    }

    @Test
    fun cpuDecoderMatchesReferencePixels() {
        BlurHashReferenceVectors.forEach { vector ->
            val blurHash = assertNotNull(BlurHash.decode(vector.blurHash, vector.punch))
            val pixels = blurHash.toArgbPixels()
            assertEquals(32 * 32, pixels.size)
            vector.forEachSample { x, y, expected ->
                val argb = pixels[y * 32 + x]
                val actual = intArrayOf(argb shr 16 and 255, argb shr 8 and 255, argb and 255, argb ushr 24)
                for (channel in 0..3) {
                    assertTrue(
                        abs(expected[channel] - actual[channel]) <= 1,
                        "${vector.blurHash} (punch ${vector.punch}) at ($x, $y): expected ${expected.toList()}, got ${actual.toList()}"
                    )
                }
            }
        }
    }
}
