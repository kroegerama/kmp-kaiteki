package com.kroegerama.kmp.kaiteki.feature

import androidx.compose.runtime.Immutable
import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi
import kotlin.io.encoding.Base64
import kotlin.math.floor
import kotlin.math.max

/**
 * A decoded [ThumbHash](https://evanw.github.io/thumbhash/) placeholder: the frequency-domain representation of a
 * small image with its aspect ratio and optional alpha channel. Obtain one with [decode].
 *
 * @property hasAlpha whether the hash encodes an alpha channel.
 * @property lx number of horizontal luminance components.
 * @property ly number of vertical luminance components.
 * @property lCoefficients luminance weights of the basis `cos(PI * cx * u) * cos(PI * cy * v)` over normalized
 *   coordinates, row by row for each `cy` and every `cx` with `cx * ly < lx * (ly - cy)`, starting with the constant term.
 * @property pCoefficients yellow minus blue weights in the same order, for a 3 by 3 triangle.
 * @property qCoefficients red minus green weights in the same order, for a 3 by 3 triangle.
 * @property aCoefficients alpha weights in the same order for a 5 by 5 triangle, or only the constant `1` without alpha.
 * @property averageColor the image's average color as a non-premultiplied ARGB integer.
 * @property aspectRatio the approximate aspect ratio (width divided by height) of the original image.
 * @property width width of the reference rendering: 32 on the long side, matching [aspectRatio].
 * @property height height of the reference rendering: 32 on the long side, matching [aspectRatio].
 */
@ExperimentalKaitekiApi
@Immutable
public data class ThumbHash(
    val hasAlpha: Boolean,
    val lx: Int,
    val ly: Int,
    val lCoefficients: FloatArray,
    val pCoefficients: FloatArray,
    val qCoefficients: FloatArray,
    val aCoefficients: FloatArray,
    val averageColor: Int,
    val aspectRatio: Float,
    val width: Int,
    val height: Int
) {
    public companion object {
        /**
         * Decodes the raw bytes of a ThumbHash, or returns `null` if they are malformed or truncated.
         *
         * @param bytes the encoded ThumbHash bytes.
         */
        public fun decode(bytes: ByteArray): ThumbHash? = ThumbHashDecoder.decode(bytes)

        /**
         * Decodes a Base64 encoded ThumbHash (standard alphabet, padding optional), or returns `null` if it is malformed.
         *
         * @param base64 the Base64 encoded ThumbHash.
         */
        public fun decode(base64: String): ThumbHash? {
            val bytes = try {
                ThumbHashDecoder.base64.decode(base64)
            } catch (_: IllegalArgumentException) {
                return null
            }
            return decode(bytes)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ThumbHash

        if (hasAlpha != other.hasAlpha) return false
        if (lx != other.lx) return false
        if (ly != other.ly) return false
        if (!lCoefficients.contentEquals(other.lCoefficients)) return false
        if (!pCoefficients.contentEquals(other.pCoefficients)) return false
        if (!qCoefficients.contentEquals(other.qCoefficients)) return false
        if (!aCoefficients.contentEquals(other.aCoefficients)) return false
        if (averageColor != other.averageColor) return false
        if (aspectRatio != other.aspectRatio) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hasAlpha.hashCode()
        result = 31 * result + lx
        result = 31 * result + ly
        result = 31 * result + lCoefficients.contentHashCode()
        result = 31 * result + pCoefficients.contentHashCode()
        result = 31 * result + qCoefficients.contentHashCode()
        result = 31 * result + aCoefficients.contentHashCode()
        result = 31 * result + averageColor
        result = 31 * result + aspectRatio.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

@OptIn(ExperimentalKaitekiApi::class)
internal object ThumbHashDecoder {
    val base64: Base64 = Base64.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

    fun decode(hash: ByteArray): ThumbHash? {
        if (hash.size < 5) return null
        val header24 = hash.u(0) or (hash.u(1) shl 8) or (hash.u(2) shl 16)
        val header16 = hash.u(3) or (hash.u(4) shl 8)
        val hasAlpha = (header24 shr 23) != 0
        if (hasAlpha && hash.size < 6) return null

        val lDc = (header24 and 63) / 63.0
        val pDc = ((header24 shr 6) and 63) / 31.5 - 1
        val qDc = ((header24 shr 12) and 63) / 31.5 - 1
        val lScale = ((header24 shr 18) and 31) / 31.0
        val pScale = ((header16 shr 3) and 63) / 63.0
        val qScale = ((header16 shr 9) and 63) / 63.0
        val isLandscape = (header16 shr 15) != 0
        val lLimit = if (hasAlpha) 5 else 7
        val rawLx = if (isLandscape) lLimit else header16 and 7
        val rawLy = if (isLandscape) header16 and 7 else lLimit
        if (rawLx == 0 || rawLy == 0) return null
        val lx = max(3, rawLx)
        val ly = max(3, rawLy)
        val aDc = if (hasAlpha) (hash.u(5) and 15) / 15.0 else 1.0
        val aScale = if (hasAlpha) (hash.u(5) shr 4) / 15.0 else 0.0

        val acStart = if (hasAlpha) 6 else 5
        val acCount = (triangleSize(lx, ly) - 1) + 2 * (triangleSize(3, 3) - 1) + (if (hasAlpha) triangleSize(5, 5) - 1 else 0)
        if (hash.size < acStart + (acCount + 1) / 2) return null

        var acIndex = 0
        fun decodeChannel(nx: Int, ny: Int, dc: Double, scale: Double): FloatArray {
            val out = FloatArray(triangleSize(nx, ny))
            out[0] = dc.toFloat()
            for (i in 1..<out.size) {
                val nibble = (hash.u(acStart + (acIndex shr 1)) shr ((acIndex and 1) shl 2)) and 15
                acIndex++
                out[i] = ((nibble / 7.5 - 1) * scale * 2).toFloat()
            }
            return out
        }

        val lCoefficients = decodeChannel(lx, ly, lDc, lScale)
        val pCoefficients = decodeChannel(3, 3, pDc, pScale * 1.25)
        val qCoefficients = decodeChannel(3, 3, qDc, qScale * 1.25)
        val aCoefficients = if (hasAlpha) decodeChannel(5, 5, aDc, aScale) else floatArrayOf(1f)

        val ratio = rawLx.toFloat() / rawLy
        val width = roundHalfUp(if (ratio > 1) 32.0 else 32.0 * ratio)
        val height = roundHalfUp(if (ratio > 1) 32.0 / ratio else 32.0)

        return ThumbHash(
            hasAlpha = hasAlpha,
            lx = lx,
            ly = ly,
            lCoefficients = lCoefficients,
            pCoefficients = pCoefficients,
            qCoefficients = qCoefficients,
            aCoefficients = aCoefficients,
            averageColor = averageColor(lDc, pDc, qDc, aDc),
            aspectRatio = ratio,
            width = width,
            height = height
        )
    }

    /** Number of `(cx, cy)` pairs with `cx * ny < nx * (ny - cy)`, the constant term included. */
    fun triangleSize(nx: Int, ny: Int): Int {
        var count = 0
        for (cy in 0..<ny) {
            var cx = 0
            while (cx * ny < nx * (ny - cy)) {
                count++
                cx++
            }
        }
        return count
    }

    private fun averageColor(l: Double, p: Double, q: Double, a: Double): Int {
        val b = l - 2.0 / 3.0 * p
        val r = (3 * l - b + q) / 2
        val g = r - q
        return (channel(a) shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }

    private fun channel(value: Double): Int = roundHalfUp(value.coerceIn(0.0, 1.0) * 255)

    private fun roundHalfUp(value: Double): Int = floor(value + 0.5).toInt()

    private fun ByteArray.u(index: Int): Int = this[index].toInt() and 255
}
