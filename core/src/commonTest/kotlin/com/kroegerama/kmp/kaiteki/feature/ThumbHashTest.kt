package com.kroegerama.kmp.kaiteki.feature

import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Expected values were produced by the reference JavaScript implementation (evanw/thumbhash). */
@OptIn(ExperimentalKaitekiApi::class)
class ThumbHashTest {

    private class Vector(
        val base64: String,
        val bytes: IntArray,
        val hasAlpha: Boolean,
        val lx: Int,
        val ly: Int,
        val averageColor: Long,
        val aspectRatio: Float,
        val width: Int,
        val height: Int
    )

    private val landscape = Vector(
        base64 = "3OcRJYB4d3h/iIeHeEh3eIhw+j3A",
        bytes = intArrayOf(220, 231, 17, 37, 128, 120, 119, 120, 127, 136, 135, 135, 120, 72, 119, 120, 136, 112, 250, 61, 192),
        hasAlpha = false, lx = 7, ly = 5, averageColor = 0xFF6A7674, aspectRatio = 1.4f, width = 32, height = 23
    )
    private val portrait = Vector(
        base64 = "1QcSHQRnh493V4dIh4eXh1h4kJUI",
        bytes = intArrayOf(213, 7, 18, 29, 4, 103, 135, 143, 119, 87, 135, 72, 135, 135, 151, 135, 88, 120, 144, 149, 8),
        hasAlpha = false, lx = 5, ly = 7, averageColor = 0xFF565258, aspectRatio = 5f / 7f, width = 23, height = 32
    )
    private val alpha = Vector(
        base64 = "YJqGPQw7sFlslqhFafSE+Q6oJ1h2iHB2Rw==",
        bytes = intArrayOf(96, 154, 134, 61, 12, 59, 176, 89, 108, 150, 168, 69, 105, 244, 132, 249, 14, 168, 39, 88, 118, 136, 112, 118, 71),
        hasAlpha = true, lx = 5, ly = 5, averageColor = 0xBBC2754E, aspectRatio = 1f, width = 32, height = 32
    )
    private val square = Vector(
        base64 = "HdgJVxZsd3iAd3d2iGiHiHh35wvWnn8K",
        bytes = intArrayOf(29, 216, 9, 87, 22, 108, 119, 120, 128, 119, 119, 118, 136, 104, 135, 136, 120, 119, 231, 11, 214, 158, 127, 10),
        hasAlpha = false, lx = 7, ly = 7, averageColor = 0xFF6D8173, aspectRatio = 1f, width = 32, height = 32
    )
    private val wide = Vector(
        base64 = "HdgJUZhrd3iAd2eHvmDt+Zc=",
        bytes = intArrayOf(29, 216, 9, 81, 152, 107, 119, 120, 128, 119, 103, 135, 190, 96, 237, 249, 151),
        hasAlpha = false, lx = 7, ly = 3, averageColor = 0xFF6D8173, aspectRatio = 7f, width = 32, height = 5
    )

    private val vectors = listOf(landscape, portrait, alpha, square, wide)

    private fun IntArray.toBytes(): ByteArray = ByteArray(size) { this[it].toByte() }

    @Test
    fun decodesReferenceVectors() {
        vectors.forEach { vector ->
            val decoded = assertNotNull(ThumbHash.decode(vector.base64), vector.base64)
            assertEquals(vector.hasAlpha, decoded.hasAlpha, vector.base64)
            assertEquals(vector.lx, decoded.lx, vector.base64)
            assertEquals(vector.ly, decoded.ly, vector.base64)
            assertEquals(vector.averageColor.toInt(), decoded.averageColor, vector.base64)
            assertEquals(vector.aspectRatio, decoded.aspectRatio, absoluteTolerance = 1e-6f, message = vector.base64)
            assertEquals(vector.width, decoded.width, vector.base64)
            assertEquals(vector.height, decoded.height, vector.base64)
        }
    }

    @Test
    fun coefficientCountsFollowTheTriangularLayout() {
        vectors.forEach { vector ->
            val decoded = assertNotNull(ThumbHash.decode(vector.base64))
            assertEquals(ThumbHashDecoder.triangleSize(decoded.lx, decoded.ly), decoded.lCoefficients.size)
            assertEquals(6, decoded.pCoefficients.size)
            assertEquals(6, decoded.qCoefficients.size)
            assertEquals(if (decoded.hasAlpha) 15 else 1, decoded.aCoefficients.size)
        }
        assertEquals(23, ThumbHashDecoder.triangleSize(7, 5))
        assertEquals(15, ThumbHashDecoder.triangleSize(5, 5))
    }

    @Test
    fun base64AndBytesDecodeEqually() {
        vectors.forEach { vector ->
            val fromBase64 = ThumbHash.decode(vector.base64)
            val fromBytes = ThumbHash.decode(vector.bytes.toBytes())
            assertNotNull(fromBase64)
            assertEquals(fromBase64, fromBytes)
            assertEquals(fromBase64.hashCode(), fromBytes.hashCode())
        }
    }

    @Test
    fun paddingIsOptional() {
        assertEquals(ThumbHash.decode(alpha.base64), ThumbHash.decode(alpha.base64.trimEnd('=')))
        assertEquals(ThumbHash.decode(wide.base64), ThumbHash.decode(wide.base64.trimEnd('=')))
    }

    @Test
    fun malformedInputReturnsNull() {
        assertNull(ThumbHash.decode(""))
        assertNull(ThumbHash.decode(ByteArray(0)))
        assertNull(ThumbHash.decode(ByteArray(4)))
        assertNull(ThumbHash.decode(landscape.bytes.toBytes().copyOf(landscape.bytes.size - 1)))
        assertNull(ThumbHash.decode(alpha.bytes.toBytes().copyOf(5)))
        assertNull(ThumbHash.decode(alpha.bytes.toBytes().copyOf(alpha.bytes.size - 1)))
        assertNull(ThumbHash.decode(landscape.base64.dropLast(4)))
    }

    @Test
    fun invalidBase64ReturnsNull() {
        assertNull(ThumbHash.decode("!!!!"))
        assertNull(ThumbHash.decode(landscape.base64.replace('/', '_')))
        assertNull(ThumbHash.decode(landscape.base64.dropLast(1) + "==="))
        assertNull(ThumbHash.decode("A"))
    }

    @Test
    fun zeroComponentCountReturnsNull() {
        val portraitWithoutColumns = portrait.bytes.toBytes().also { it[3] = (it[3].toInt() and 7.inv()).toByte() }
        assertNull(ThumbHash.decode(portraitWithoutColumns))
        val landscapeWithoutRows = landscape.bytes.toBytes().also { it[3] = (it[3].toInt() and 7.inv()).toByte() }
        assertNull(ThumbHash.decode(landscapeWithoutRows))
    }
}
