package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.annotation.FloatRange
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

private const val BlurHashBitmapSize = 32

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
 * @param fallback color drawn when [blurHash] is `null`.
 * @param intrinsicSize the painter's intrinsic size, or [Size.Unspecified] to leave it unsized.
 */
@ExperimentalKaitekiApi
@Composable
public fun rememberBlurHashPainter(
    blurHash: BlurHash?,
    fallback: Color = Color.Transparent,
    intrinsicSize: Size = Size.Unspecified
): Painter = remember(blurHash, fallback, intrinsicSize) {
    blurHash ?: return@remember ColorPainter(fallback)
    blurHash.toPainter(intrinsicSize)
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

private data class BlurHashElement(
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
}

private class BlurHashNode(
    private var blurHash: String,
    private var punch: Float,
    private var fallback: Color
) : Modifier.Node(), DrawModifierNode {

    private var painter: Painter? = createBlurHashPainter(blurHash, punch)

    fun update(
        blurHash: String,
        punch: Float,
        fallback: Color
    ) {
        var invalidate = false
        if (this.blurHash != blurHash || this.punch != punch) {
            this.blurHash = blurHash
            this.punch = punch
            painter = createBlurHashPainter(blurHash, punch)
            invalidate = true
        }
        if (this.fallback != fallback) {
            this.fallback = fallback
            invalidate = true
        }
        if (invalidate) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val painter = painter
        if (painter != null) {
            with(painter) { draw(size) }
        } else {
            drawRect(fallback)
        }
        drawContent()
    }
}

private fun createBlurHashPainter(blurHash: String, punch: Float): Painter? =
    BlurHash.decode(blurHash, punch)?.toPainter(Size.Unspecified)

private fun BlurHash.toPainter(intrinsicSize: Size): Painter = PlaceholderBitmapPainter(
    image = createImageBitmap(toArgbPixels(), BlurHashBitmapSize, BlurHashBitmapSize),
    intrinsicSize = intrinsicSize
)

/** Decodes a [width] by [height] rendering as opaque ARGB pixels, row by row, matching the reference BlurHash decoder. */
internal fun BlurHash.toArgbPixels(
    width: Int = BlurHashBitmapSize,
    height: Int = BlurHashBitmapSize
): IntArray {
    val cosX = DoubleArray(width * componentsX) { cos(PI * (it / componentsX) * (it % componentsX) / width) }
    val cosY = DoubleArray(height * componentsY) { cos(PI * (it / componentsY) * (it % componentsY) / height) }
    val pixels = IntArray(width * height)
    for (y in 0..<height) {
        for (x in 0..<width) {
            var r = 0.0
            var g = 0.0
            var b = 0.0
            for (j in 0..<componentsY) {
                val basisY = cosY[y * componentsY + j]
                for (i in 0..<componentsX) {
                    val basis = cosX[x * componentsX + i] * basisY
                    val index = (i + j * componentsX) * 3
                    r += factors[index] * basis
                    g += factors[index + 1] * basis
                    b += factors[index + 2] * basis
                }
            }
            pixels[y * width + x] = (0xFF shl 24) or (linearToSrgb(r) shl 16) or (linearToSrgb(g) shl 8) or linearToSrgb(b)
        }
    }
    return pixels
}

private fun linearToSrgb(value: Double): Int {
    val v = value.coerceIn(0.0, 1.0)
    return if (v <= 0.0031308) {
        v * 12.92 * 255 + 0.5
    } else {
        (1.055 * v.pow(1 / 2.4) - 0.055) * 255 + 0.5
    }.toInt()
}

@OptIn(ExperimentalKaitekiApi::class)
@Preview
@Composable
private fun BlurHashModifierPreview() {
    val blurHash = "LGF5?xYk^6#M@-5c,1J5@[or[Q6."
    Grid(
        config = {
            column(1.fr)
            column(1.fr)
            gap(4.dp)
        },
        modifier = Modifier.safeDrawingPadding()
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

@OptIn(ExperimentalKaitekiApi::class)
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
        },
        modifier = Modifier.safeDrawingPadding()
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

@OptIn(ExperimentalKaitekiApi::class)
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
        },
        modifier = Modifier.safeDrawingPadding()
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
