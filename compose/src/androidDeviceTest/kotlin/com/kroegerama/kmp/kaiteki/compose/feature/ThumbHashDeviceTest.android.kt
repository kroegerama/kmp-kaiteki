package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi
import com.kroegerama.kmp.kaiteki.feature.ThumbHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Renders the ThumbHash painter, modifier and bitmap on a device and compares them with the decoded reference pixels. */
@OptIn(ExperimentalKaitekiApi::class)
class ThumbHashDeviceTest {

    private val opaque = "3OcRJYB4d3h/iIeHeEh3eIhw+j3A"
    private val withAlpha = "YJqGPQw7sFlslqhFafSE+Q6oJ1h2iHB2Rw=="

    private fun decode(base64: String): ThumbHash = requireNotNull(ThumbHash.decode(base64))

    @Test
    fun painterRendersReferencePixelsAtEverySize() = runComposeUiTest {
        val thumbHash = decode(opaque)
        setContent {
            val density = LocalDensity.current
            val painter = rememberThumbHashPainter(thumbHash)
            Column {
                listOf(1, 3).forEach { scale ->
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .testTag("painter$scale")
                            .size(with(density) { (thumbHash.width * scale).toDp() }, with(density) { (thumbHash.height * scale).toDp() })
                    )
                }
                Box(
                    Modifier
                        .testTag("modifier")
                        .size(with(density) { thumbHash.width.toDp() }, with(density) { thumbHash.height.toDp() })
                        .thumbHash(opaque)
                )
            }
        }
        assertMatches(thumbHash, onNodeWithTag("painter1").captureToImage().toPixelMap(), scale = 1)
        assertMatches(thumbHash, onNodeWithTag("painter3").captureToImage().toPixelMap(), scale = 3)
        assertMatches(thumbHash, onNodeWithTag("modifier").captureToImage().toPixelMap(), scale = 1)
    }

    @Test
    fun painterDrawsPremultipliedAlpha() = runComposeUiTest {
        val thumbHash = decode(withAlpha)
        setContent {
            val density = LocalDensity.current
            Box(
                Modifier
                    .testTag("alpha")
                    .size(with(density) { thumbHash.width.toDp() }, with(density) { thumbHash.height.toDp() })
                    .background(Color.Black)
                    .thumbHash(withAlpha)
            )
        }
        assertMatches(thumbHash, onNodeWithTag("alpha").captureToImage().toPixelMap(), scale = 1)
    }

    @Test
    fun imageBitmapHoldsTheDecodedPixels() {
        listOf(opaque, withAlpha).forEach { base64 ->
            val thumbHash = decode(base64)
            val expected = thumbHash.toArgbPixels()
            val pixels = thumbHash.toImageBitmap().toPixelMap()
            assertEquals(thumbHash.width, pixels.width)
            assertEquals(thumbHash.height, pixels.height)
            for (y in 0..<pixels.height) {
                for (x in 0..<pixels.width) {
                    val argb = expected[y * pixels.width + x]
                    val alpha = (argb ushr 24) / 255f
                    val actual = pixels[x, y]
                    assertTrue("$base64 alpha at ($x, $y)", abs(actual.alpha - alpha) <= 1.5f / 255f)
                    val expectedRed = (argb shr 16 and 255) / 255f * alpha
                    assertTrue("$base64 red at ($x, $y)", abs(actual.red * actual.alpha - expectedRed) <= 1.5f / 255f)
                }
            }
        }
    }

    /** Compares each reference pixel, composited over black, with the pixel at the matching center of a [scale]d rendering. */
    private fun assertMatches(thumbHash: ThumbHash, pixels: PixelMap, scale: Int) {
        assertEquals(thumbHash.width * scale, pixels.width)
        assertEquals(thumbHash.height * scale, pixels.height)
        val expected = thumbHash.toArgbPixels()
        for (y in 0..<thumbHash.height) {
            for (x in 0..<thumbHash.width) {
                val argb = expected[y * thumbHash.width + x]
                val alpha = (argb ushr 24) / 255f
                val actual = pixels[x * scale + scale / 2, y * scale + scale / 2]
                listOf(
                    (argb shr 16 and 255) to actual.red,
                    (argb shr 8 and 255) to actual.green,
                    (argb and 255) to actual.blue
                ).forEach { (component, actualComponent) ->
                    val expectedValue = component * alpha
                    assertTrue(
                        "scale $scale at ($x, $y): expected $expectedValue, got ${actualComponent * 255f}",
                        abs(expectedValue - actualComponent * 255f) <= 3f
                    )
                }
            }
        }
    }
}
