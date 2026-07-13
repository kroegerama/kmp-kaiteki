package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.annotation.FloatRange
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi
import com.kroegerama.kmp.kaiteki.feature.BlurHash

internal expect fun createBlurHashBrush(
    decoded: BlurHash,
    intrinsicSize: Size
): Brush

/**
 * Decodes [blurHash] and remembers the result across recompositions.
 *
 * @param blurHash the encoded [BlurHash](https://blurha.sh) string.
 * @param punch contrast multiplier applied to the color components; `1` keeps the original
 *   contrast, higher values increase it.
 * @return the decoded [BlurHash], or `null` if [blurHash] is malformed.
 */
@ExperimentalKaitekiApi
@Composable
public fun rememberBlurHash(
    blurHash: String,
    @FloatRange(from = 0.0)
    punch: Float = 1f
): BlurHash? = remember(blurHash, punch) {
    BlurHash.decode(blurHash, punch)
}

/**
 * Decodes [blurHash] and remembers a [Painter] that renders it.
 *
 * @param blurHash the encoded [BlurHash](https://blurha.sh) string.
 * @param punch contrast multiplier applied to the color components; `1` keeps the original
 *   contrast, higher values increase it.
 * @param fallback color drawn when [blurHash] is malformed.
 * @param intrinsicSize the painter's intrinsic size, or [Size.Unspecified] to leave it unsized.
 */
@ExperimentalKaitekiApi
@Composable
public fun rememberBlurHashPainter(
    blurHash: String,
    @FloatRange(from = 0.0)
    punch: Float = 1f,
    fallback: Color = Color.Transparent,
    intrinsicSize: Size = Size.Unspecified
): Painter = rememberBlurHashPainter(
    blurHash = rememberBlurHash(blurHash, punch),
    fallback = fallback,
    intrinsicSize = intrinsicSize
)

/**
 * Remembers a [Painter] that renders the already-decoded [blurHash].
 *
 * @param blurHash the decoded [BlurHash], or `null` to draw [fallback].
 * @param fallback color drawn when [blurHash] is `null`; defaults to the hash's average color.
 * @param intrinsicSize the painter's intrinsic size, or [Size.Unspecified] to leave it unsized.
 */
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

/**
 * Draws the decoded [blurHash] behind the content, filling the layout bounds.
 *
 * @param blurHash the encoded [BlurHash](https://blurha.sh) string.
 * @param punch contrast multiplier applied to the color components; `1` keeps the original
 *   contrast, higher values increase it.
 * @param fallback color drawn when [blurHash] is malformed.
 */
@ExperimentalKaitekiApi
public fun Modifier.blurHash(
    blurHash: String,
    @FloatRange(from = 0.0)
    punch: Float = 1f,
    fallback: Color = Color.Transparent
): Modifier = this then BlurHashElement(
    blurHash = blurHash,
    punch = punch,
    fallback = fallback
)

private class BlurHashElement(
    val blurHash: String,
    val punch: Float,
    val fallback: Color
) : ModifierNodeElement<BlurHashNode>() {

    override fun create() = BlurHashNode(blurHash, punch, fallback)

    override fun update(node: BlurHashNode) {
        node.update(blurHash, punch, fallback)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "blurHash"
        properties["blurHash"] = blurHash
        properties["punch"] = punch
        properties["fallback"] = fallback
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurHashElement) return false
        return blurHash == other.blurHash &&
                punch == other.punch &&
                fallback == other.fallback
    }

    override fun hashCode(): Int {
        var result = blurHash.hashCode()
        result = 31 * result + punch.hashCode()
        result = 31 * result + fallback.hashCode()
        return result
    }
}

private class BlurHashNode(
    private var blurHash: String,
    private var punch: Float,
    private var fallback: Color
) : Modifier.Node(), DrawModifierNode {

    // decoded once per hash/punch change; the shader itself is compiled lazily and cached by the
    // ShaderBrush per draw size, so the draw path stays a single drawRect
    private var brush: Brush? = null

    override fun onAttach() {
        rebuildBrush()
    }

    fun update(
        blurHash: String,
        punch: Float,
        fallback: Color
    ) {
        var invalidate = false
        if (this.blurHash != blurHash || this.punch != punch) {
            this.blurHash = blurHash
            this.punch = punch
            rebuildBrush()
            invalidate = true
        }
        if (this.fallback != fallback) {
            this.fallback = fallback
            invalidate = true
        }
        if (invalidate) invalidateDraw()
    }

    private fun rebuildBrush() {
        val decoded = BlurHash.decode(blurHash, punch)
        brush = decoded?.let { createBlurHashBrush(it, Size.Unspecified) }
    }

    override fun ContentDrawScope.draw() {
        val brush = brush
        if (brush != null) {
            drawRect(brush)
        } else {
            drawRect(fallback)
        }
        drawContent()
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
        listOf(0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f).fastForEach { punch ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .blurHash(blurHash, punch)
            )
        }
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
        listOf(0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f).fastForEach { punch ->
            Image(
                painter = rememberBlurHashPainter(blurHash, punch),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
            )
        }
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
