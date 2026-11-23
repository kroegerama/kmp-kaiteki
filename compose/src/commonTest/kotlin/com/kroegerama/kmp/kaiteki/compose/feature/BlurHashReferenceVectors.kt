package com.kroegerama.kmp.kaiteki.compose.feature

/**
 * Output of `decode` from the reference TypeScript implementation (woltapp/blurhash) at 32x32, with [punch] applied as a plain multiplier.
 *
 * @property blurHash the encoded hash.
 * @property punch contrast multiplier passed to the decoder.
 * @property samples RGBA hex values of the pixels at every combination of [samplePositions] for y (outer) and x (inner).
 */
internal class BlurHashReferenceVector(
    val blurHash: String,
    val punch: Float,
    val samples: String
) {
    val samplePositions: List<Int> = listOf(0, 8, 16, 24, 31)

    /** Visits each sample as its position and RGBA components. */
    fun forEachSample(block: (x: Int, y: Int, rgba: IntArray) -> Unit) {
        var offset = 0
        samplePositions.forEach { y ->
            samplePositions.forEach { x ->
                val rgba = IntArray(4) { samples.substring(offset + it * 2, offset + it * 2 + 2).toInt(16) }
                block(x, y, rgba)
                offset += 8
            }
        }
    }
}

internal val BlurHashReferenceVectors: List<BlurHashReferenceVector> = listOf(
    BlurHashReferenceVector(
        blurHash = "00F5?x",
        punch = 1f,
        samples = "837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff" +
            "837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff837d98ff"
    ),
    BlurHashReferenceVector(
        blurHash = "1GF5?xYk",
        punch = 1f,
        samples = "8292adff828ca7ff837d98ff846b87ff84637fff8292adff828ca7ff837d98ff846b87ff84637fff8292adff828ca7ff837d98ff" +
            "846b87ff84637fff8292adff828ca7ff837d98ff846b87ff84637fff8292adff828ca7ff837d98ff846b87ff84637fff"
    ),
    BlurHashReferenceVector(
        blurHash = "9CF5?x_{",
        punch = 1f,
        samples = "966fa5ff966fa5ff966fa5ff966fa5ff966fa5ff9173a1ff9173a1ff9173a1ff9173a1ff9173a1ff837d98ff837d98ff837d98ff" +
            "837d98ff837d98ff73868eff73868eff73868eff73868eff73868eff6c8a8aff6c8a8aff6c8a8aff6c8a8aff6c8a8aff"
    ),
    BlurHashReferenceVector(
        blurHash = "8GF5?xYk^6#M9vF~W@j=#*",
        punch = 1f,
        samples = "8a8fa6ff9b93aaff616c87ff98757eff915f8eff8a8fa6ff9b93aaff616c87ff98757eff915f8eff8a8fa6ff9b93aaff616c87ff" +
            "98757eff915f8eff8a8fa6ff9b93aaff616c87ff98757eff915f8eff8a8fa6ff9b93aaff616c87ff98757eff915f8eff"
    ),
    BlurHashReferenceVector(
        blurHash = "=HF5?x@-@[};jMj]beXmxH",
        punch = 1f,
        samples = "c07bb4ffc07bb4ffc07bb4ffc07bb4ffc07bb4ff83709fff83709fff83709fff83709fff83709fff7185a2ff7185a2ff7185a2ff" +
            "7185a2ff7185a2ff8d8e86ff8d8e86ff8d8e86ff8d8e86ff8d8e86ff696f8bff696f8bff696f8bff696f8bff696f8bff"
    ),
    BlurHashReferenceVector(
        blurHash = "|HF5?xYk^6#M9wKSW@j=#*@-5b,1J5O[V=R:s;w[@[or[k6.O[TLtJnNnO};FxngOZE3NgNHsps,jMFxS#OtcXnzRjxZxHj]OYNe" +
            $$"R:JCs9xunhwIbeIpNaxHNGr;v}aeo0Xmt6XS$et6#*$ft6nhxHnNV@w{nOenwfNHo0",
        punch = 1f,
        samples = "b08bc8ffe581a0ff8778c5fff66a84fff36a9affbc6a95ff7986bcff425480ff956c66ff9362b0ff809cbfff729fccff828676ff" +
            "957170ff6070b7ff857275ffacb593ff605d61ffa7a297ff855752ff8d9372ff836c6eff1f71adff5b61a4ff864b53ff"
    ),
    BlurHashReferenceVector(
        blurHash = "LGF5?xYk^6#M@-5c,1J5@[or[Q6.",
        punch = 0.5f,
        samples = "9c799eff977aa9ff7d71a1ff97688cffba6a8dff9281a4ff8480afff7b7698ff907087ff9d749eff868da5ff6a89b0ff787d8eff" +
            "847982ff687eaaff8f908fff7b8c9eff747e93ff7b7686ff737796ff98907dff8c8b90ff727d9aff79728bff896f81ff"
    ),
    BlurHashReferenceVector(
        blurHash = "LGF5?xYk^6#M@-5c,1J5@[or[Q6.",
        punch = 1f,
        samples = "b075a3ffa776b8ff7663a9ffa74d7effe15381ff9e85afff8582c3ff736e98ff9c6273ffb36aa3ff899ab1ff4594c5ff6b7d82ff" +
            "857567ff3e7fbaff9aa186ff7298a5ff627f8eff736f71ff5f7194ffaba058ff949787ff5e7d9cff6e647bff8f5f62ff"
    ),
    BlurHashReferenceVector(
        blurHash = "LGF5?xYk^6#M@-5c,1J5@[or[Q6.",
        punch = 2f,
        samples = "d26cadffc46fd3ff673bb7ffc4005affff0064ffb58cc3ff8688e4ff5f5c99ffb23630ffd652adff8eb2c6ff00a7e7ff487d66ff" +
            "866d00ff0080d5ffaebd70ff5dafb0ff218284ff5f5e25ff03638fffc9bb00ffa2ad73ff007da0ff544052ff9a2c00ff"
    ),
    BlurHashReferenceVector(
        blurHash = $$"|GF~_=x@_IV]Dlx@xskCocQ_WEt1WXjGWVt4WCf6xvV{MhbXxrRkoJbHWCVzX7jZa$t6oMobWEWCs;W-RkkCocWCWUWUjZtQoMWC" +
            "j[s.bFagj[oLM~oeoba}t6aiWBa#t6WWX7ocWUk8jaWCj@t6M~WCoef8aybHjuofWV",
        punch = 1f,
        samples = "9fa57bff88927fff747179ff868b7affa6945dff99a85fff94a167ff1b2c2bff8e9c58ff8c954bff99b058ff97a754ff483918ff" +
            "9ba45fff889346ffa4b173ff90a65cff546732ff8da152ff879949ff98ac5fff99ac60ff758b42ff92a65dff8a9a55ff"
    )
)
