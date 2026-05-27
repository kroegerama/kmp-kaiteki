package com.kroegerama.kmp.kaiteki.feature

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import kotlin.math.pow
import kotlin.math.withSign

@Immutable
public data class BlurHash(
    val componentsX: Int,
    val componentsY: Int,
    val averageColor: Int,
    val factors: FloatArray
) {
    public companion object {
        public fun decode(
            blurHash: String,
            @FloatRange(0.0, 1.0)
            punch: Float = 1f
        ): BlurHash? = BlurHashDecoder.decode(
            blurHash = blurHash,
            punch = punch
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BlurHash

        if (componentsX != other.componentsX) return false
        if (componentsY != other.componentsY) return false
        if (averageColor != other.averageColor) return false
        if (!factors.contentEquals(other.factors)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = componentsX
        result = 31 * result + componentsY
        result = 31 * result + averageColor
        result = 31 * result + factors.contentHashCode()
        return result
    }
}

internal object BlurHashDecoder {
    fun decode(
        blurHash: String,
        punch: Float
    ): BlurHash? {
        if (blurHash.length < 6) {
            return null
        }

        val sizeFlag = decode83(blurHash, 0, 1)
        val componentsX = (sizeFlag % 9) + 1
        val componentsY = (sizeFlag / 9) + 1

        if (blurHash.length != 4 + 2 * componentsX * componentsY) {
            return null
        }

        val maxAcQuant = decode83(blurHash, 1, 2)
        val maxAc = (maxAcQuant + 1) / 166f

        val factors = FloatArray(componentsX * componentsY * 3)

        val dcQuant = decode83(blurHash, 2, 6)
        decodeDc(dcQuant, factors)
        val dcR = linearToSrgb(factors[0])
        val dcG = linearToSrgb(factors[1])
        val dcB = linearToSrgb(factors[2])
        val averageColor = 255 shl 24 or (dcR shl 16) or (dcG shl 8) or dcB

        var idx = 1
        while (idx < componentsX * componentsY) {
            val start = 4 + idx * 2
            val acQuant = decode83(blurHash, start, start + 2)
            decodeAc(acQuant, maxAc * punch, factors, idx * 3)
            idx++
        }

        return BlurHash(
            componentsX = componentsX,
            componentsY = componentsY,
            averageColor = averageColor,
            factors = factors
        )
    }

    private fun decode83(chars: String, start: Int, end: Int): Int {
        var result = 0
        for (i in start..<end) {
            result = result * 83 + BASE_83_CHARS.indexOf(chars[i])
        }
        return result
    }

    private fun srgbToLinear(value: Int): Float {
        val v = value / 255f
        return if (v <= 0.04045f) {
            v / 12.92f
        } else {
            ((v + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    private fun linearToSrgb(value: Float): Int {
        val v = value.coerceIn(0f, 1f)
        return if (v <= 0.0031308f) {
            v * 12.92f * 255f + 0.5f
        } else {
            (1.055f * v.pow(1 / 2.4f) - 0.055f) * 255 + 0.5f
        }.toInt()
    }

    private fun signedPow2(value: Float) = (value * value).withSign(value)

    private fun decodeDc(
        value: Int,
        out: FloatArray
    ) {
        out[0] = srgbToLinear(value shr 16)
        out[1] = srgbToLinear((value shr 8) and 255)
        out[2] = srgbToLinear(value and 255)
    }

    private fun decodeAc(
        value: Int,
        maxAc: Float,
        out: FloatArray,
        offset: Int
    ) {
        val qR = value / (19 * 19)
        val qG = (value / 19) % 19
        val qB = value % 19
        out[offset + 0] = signedPow2((qR - 9) / 9f) * maxAc
        out[offset + 1] = signedPow2((qG - 9) / 9f) * maxAc
        out[offset + 2] = signedPow2((qB - 9) / 9f) * maxAc
    }

    private const val BASE_83_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~"
}
