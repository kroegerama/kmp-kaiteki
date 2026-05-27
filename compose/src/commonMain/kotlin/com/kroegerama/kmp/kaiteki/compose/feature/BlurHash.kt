package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.annotation.FloatRange
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi
import com.kroegerama.kmp.kaiteki.feature.BlurHash

internal expect fun createBlurHashBrush(
    decoded: BlurHash,
    intrinsicSize: Size
): Brush

@ExperimentalKaitekiApi
@Composable
public fun rememberBlurHash(
    blurHash: String,
    @FloatRange(0.0, 1.0)
    punch: Float = 1f
): BlurHash? = remember(blurHash, punch) {
    BlurHash.decode(blurHash, punch)
}

@ExperimentalKaitekiApi
@Composable
public fun rememberBlurHashPainter(
    blurHash: String,
    @FloatRange(0.0, 1.0)
    punch: Float = 1f,
    fallback: Color = Color.Transparent,
    intrinsicSize: Size = Size.Unspecified
): Painter = rememberBlurHashPainter(
    blurHash = rememberBlurHash(blurHash, punch),
    fallback = fallback,
    intrinsicSize = intrinsicSize
)

@ExperimentalKaitekiApi
@Composable
public fun rememberBlurHashPainter(
    blurHash: BlurHash?,
    fallback: Color = blurHash?.averageColor?.let(::Color) ?: Color.Transparent,
    intrinsicSize: Size = Size.Unspecified
): Painter = remember(blurHash, fallback, intrinsicSize) {
    blurHash ?: return@remember ColorPainter(fallback)
    BrushPainter(createBlurHashBrush(blurHash, intrinsicSize))
}

@ExperimentalKaitekiApi
public fun Modifier.blurHash(
    blurHash: String,
    @FloatRange(0.0, 1.0)
    punch: Float = 1f,
    fallback: Color = Color.Transparent
): Modifier = composed(
    fullyQualifiedName = "com.kroegerama.kmp.kaiteki.compose.feature",
    blurHash,
    punch,
    fallback,
    inspectorInfo = debugInspectorInfo {
        name = "blurHash"
        properties["blurHash"] = blurHash
        properties["punch"] = punch
        properties["fallback"] = fallback
    },
) {
    val brush = remember(blurHash, punch) {
        val decoded = BlurHash.decode(blurHash, punch) ?: return@remember null
        createBlurHashBrush(decoded, Size.Unspecified)
    } ?: return@composed background(fallback)
    drawWithCache {
        onDrawBehind {
            drawRect(brush)
        }
    }
}

/**
 * this shader works with AGSL (Android) and SKSL (SKIA)
 */
internal fun buildBlurHashShaderSource(
    componentsX: Int,
    componentsY: Int
): String = buildString {
    val factorCount = componentsX * componentsY
    appendLine(
        """
        uniform float2 resolution;
        uniform half3 factors[$factorCount];
        
        const half PI = 3.14159265;
        
        half3 linearToSrgb(half3 v) {
            half3 cutoff = step(v, half3(0.0031308));
        
            half3 lower = v * 12.92;
            half3 higher = 1.055 * pow(v, half3(1.0 / 2.4)) - 0.055;
        
            return mix(higher, lower, cutoff);
        }
        
        half4 main(float2 fragCoord) {
            half2 uv = half2(fragCoord / resolution);
            
    """.trimIndent()
    )

    for (x in 0 until componentsX) {
        when (x) {
            0 -> appendLine("    half x0 = 1.0;")
            1 -> appendLine("    half x1 = cos(PI * uv.x);")
            else -> appendLine("    half x$x = cos(${x}.0 * PI * uv.x);")
        }
    }

    appendLine()

    for (y in 0 until componentsY) {
        when (y) {
            0 -> appendLine("    half y0 = 1.0;")
            1 -> appendLine("    half y1 = cos(PI * uv.y);")
            else -> appendLine("    half y$y = cos(${y}.0 * PI * uv.y);")
        }
    }

    appendLine()
    appendLine("    half3 color = half3(0.0);")
    appendLine()

    var index = 0

    for (y in 0 until componentsY) {
        for (x in 0 until componentsX) {
            appendLine("    color += factors[$index] * (x$x * y$y);")
            index++
        }
    }

    appendLine()
    appendLine(
        """
            color = linearToSrgb(color);
            return half4(saturate(color), 1.0);
        }
    """.trimIndent()
    )
}

@OptIn(ExperimentalGridApi::class, ExperimentalKaitekiApi::class)
@Preview
@Composable
private fun BlurHashModifierPreview() {
    val blurHash = "LGF5?xYk^6#M@-5c,1J5@[or[Q6."
    Grid(
        config = {
            column(1.fr)
            column(1.fr)
            gap(4.dp)
        }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .blurHash(blurHash)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .blurHash(blurHash, .75f)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .blurHash(blurHash, .5f)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .blurHash(blurHash, .25f)
        )
    }
}

@OptIn(ExperimentalGridApi::class, ExperimentalKaitekiApi::class)
@Preview
@Composable
private fun BlurHashPainterPreview() {
    val blurHash =
        "|GF~_=x@_IV]Dlx@xskCocQ_WEt1WXjGWVt4WCf6xvV{MhbXxrRkoJbHWCVzX7jZa\$t6oMobWEWCs;W-RkkCocWCWUWUjZtQoMWCj[s.bFagj[oLM~oeoba}t6aiWBa#t6WWX7ocWUk8jaWCj@t6M~WCoef8aybHjuofWV"
    Grid(
        config = {
            column(1.fr)
            column(1.fr)
            gap(4.dp)
        }
    ) {
        Image(
            painter = rememberBlurHashPainter(blurHash),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
        )
        Image(
            painter = rememberBlurHashPainter(blurHash, .75f),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
        )
        Image(
            painter = rememberBlurHashPainter(blurHash, .5f),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
        )
        Image(
            painter = rememberBlurHashPainter(blurHash, .25f),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
        )
    }
}

@OptIn(ExperimentalGridApi::class, ExperimentalKaitekiApi::class)
@Preview
@Composable
private fun BlurHashSizesPreview() {
    val hashes = listOf(
        "1x1" to "00F5?x",
        "2x1" to "1GF5?xYk",
        "1x2" to "9CF5?x_{",
        "9x1" to "8GF5?xYk^6#M9vF~W@j=#*",
        "1x9" to "=HF5?x@-@[};jMj]beXmxH",
        "9x9" to "|HF5?xYk^6#M9wKSW@j=#*@-5b,1J5O[V=R:s;w[@[or[k6.O[TLtJnNnO};FxngOZE3NgNHsps,jMFxS#OtcXnzRjxZxHj]OYNeR:JCs9xunhwIbeIpNaxHNGr;v}aeo0Xmt6XS\$et6#*\$ft6nhxHnNV@w{nOenwfNHo0",
    )
    Grid(
        config = {
            column(1.fr)
            column(1.fr)
            gap(4.dp)
        }
    ) {
        hashes.fastForEach { (title, hash) ->
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .blurHash(hash, .8f)
            )
        }
    }
}
