package com.kroegerama.kmp.kaiteki.compose.feature

/**
 * Output of `thumbHashToRGBA` from the reference JavaScript implementation (evanw/thumbhash).
 *
 * @property base64 the encoded hash.
 * @property width width of the reference rendering.
 * @property height height of the reference rendering.
 * @property samples RGBA hex values of the pixels at every combination of [sampleYs] (outer) and [sampleXs] (inner).
 */
internal class ThumbHashReferenceVector(
    val base64: String,
    val width: Int,
    val height: Int,
    val samples: String
) {
    val sampleXs: List<Int> = listOf(0, width shr 2, width shr 1, (3 * width) shr 2, width - 1)
    val sampleYs: List<Int> = listOf(0, height shr 2, height shr 1, (3 * height) shr 2, height - 1)

    /** Visits each sample as its position and non-premultiplied RGBA components. */
    fun forEachSample(block: (x: Int, y: Int, rgba: IntArray) -> Unit) {
        var offset = 0
        sampleYs.forEach { y ->
            sampleXs.forEach { x ->
                val rgba = IntArray(4) { samples.substring(offset + it * 2, offset + it * 2 + 2).toInt(16) }
                block(x, y, rgba)
                offset += 8
            }
        }
    }
}

internal val ThumbHashReferenceVectors: List<ThumbHashReferenceVector> = listOf(
    ThumbHashReferenceVector(
        base64 = "3OcRJYB4d3h/iIeHeEh3eIhw+j3A",
        width = 32,
        height = 23,
        samples = "7c889fff838fa8ff808ca8ff929eb6ff7c889fffa5b1c8ffa8b4ceff96a3c0ffa6b3ceff8794acff5a666cff717d87ff707c8bff" +
            "7d8996ff586470ff222e06ff394522ff364224ff212d10ff192507ff5a6727ff677338ff5d6934ff424e1aff56632eff"
    ),
    ThumbHashReferenceVector(
        base64 = "1QcSHQRnh493V4dIh4eXh1h4kJUI",
        width = 23,
        height = 32,
        samples = "404d71ff5b6480ff72778bff5e6f87ff546d8bff857e8bffa3979dffb7a7a5ff959093ff8e919aff533e37ff705548ff8c6d58ff" +
            "5a4638ff564841ff080c19ff07060dff2b2527ff1d212bff293445ff000c2aff000019ff152236ff000622ff000427ff"
    ),
    ThumbHashReferenceVector(
        base64 = "YJqGPQw7sFlslqhFafSE+Q6oJ1h2iHB2Rw==",
        width = 32,
        height = 32,
        samples = "e44b3300aa5f651871848645acb25940c0a71700eb56438caf6871ff788d92ffc2ca73ffe1ca3b80e93c2d9faa4a57ff6b666dff" +
            "bda953ffe7b32594ff2b1773db3e45ff985354ffe18a2dffff92007cff210900f8393d6db8514d85fc832159ff880000"
    ),
    ThumbHashReferenceVector(
        base64 = "mYqDBQQnxnj0JoLYdN7f8JhpuDeHiHdwZw==",
        width = 32,
        height = 32,
        samples = "f1112c14ec162c87e31f2c53db272c81d9292c14f2102c90ed152cb2e51e2c33dd252ca7db272c7cf1112cabed152c95e51d2c00" +
            "df232c96dd252ca4ec162c68e81a2cafe1212c45dc262cabdb272c7be9192c14e61d2ca1df232c6dda282c8cd9292c26"
    ),
    ThumbHashReferenceVector(
        base64 = "HdgJVxZsd3iAd3d2iGiHiHh35wvWnn8K",
        width = 32,
        height = 32,
        samples = "e2000fffb50047ff65007bff430368ff390046ffc92545ffa83a8bff4b32b8ff2432a5ff1a2c86ffba8257ff94949eff338dd4ff" +
            "068ac6ff007ba1ffeed500ffadcd26ff53d170ff2fdc77ff21d45cffffff00ffe7ff00ff8eff4cff57ff44ff47f729ff"
    ),
    ThumbHashReferenceVector(
        base64 = "HdgJUZhrd3iAd2eHvmDt+Zc=",
        width = 32,
        height = 5,
        samples = "e30019ffbd0b5bff630d8aff45187cff390f59ffc82048ffa0338aff4537bfff203db0ff12328dffc06b55ff917995ff357fd3ff" +
            "0982c8ff0077a7ffe1be15ffa9c550ff4ccd98ff1fd195ff11ca79fffff600ffccf600ff6eff4aff43ff4fff38ff39ff"
    )
)
