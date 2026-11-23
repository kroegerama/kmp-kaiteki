package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi
import com.kroegerama.kmp.kaiteki.compose.modifier.checkerboard
import com.kroegerama.kmp.kaiteki.feature.ThumbHash
import kotlin.math.PI
import kotlin.math.cos

/** Creates an [ImageBitmap] from non-premultiplied ARGB [pixels], stored row by row. */
internal expect fun createImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int
): ImageBitmap

/**
 * Decodes the Base64 encoded [thumbHash] and remembers the result across recompositions.
 *
 * @param thumbHash the Base64 encoded [ThumbHash](https://evanw.github.io/thumbhash/).
 * @return the decoded [ThumbHash], or `null` if [thumbHash] is malformed.
 */
@ExperimentalKaitekiApi
@Composable
public fun rememberThumbHash(
    thumbHash: String
): ThumbHash? = remember(thumbHash) {
    ThumbHash.decode(thumbHash)
}

/**
 * Decodes the raw [thumbHash] bytes and remembers the result for this array instance.
 *
 * @param thumbHash the raw [ThumbHash](https://evanw.github.io/thumbhash/) bytes.
 * @return the decoded [ThumbHash], or `null` if [thumbHash] is malformed.
 */
@ExperimentalKaitekiApi
@Composable
public fun rememberThumbHash(
    thumbHash: ByteArray
): ThumbHash? = remember(thumbHash) {
    ThumbHash.decode(thumbHash)
}

/**
 * Decodes the Base64 encoded [thumbHash] and remembers a [Painter] that renders it, sized to the hash's reference size.
 *
 * @param thumbHash the Base64 encoded [ThumbHash](https://evanw.github.io/thumbhash/).
 * @param fallback color drawn when [thumbHash] is malformed.
 */
@ExperimentalKaitekiApi
@Composable
public fun rememberThumbHashPainter(
    thumbHash: String,
    fallback: Color = Color.Transparent
): Painter = rememberThumbHashPainter(
    thumbHash = rememberThumbHash(thumbHash),
    fallback = fallback
)

/**
 * Decodes the raw [thumbHash] bytes and remembers a [Painter] that renders it, sized to the hash's reference size.
 *
 * @param thumbHash the raw [ThumbHash](https://evanw.github.io/thumbhash/) bytes.
 * @param fallback color drawn when [thumbHash] is malformed.
 */
@ExperimentalKaitekiApi
@Composable
public fun rememberThumbHashPainter(
    thumbHash: ByteArray,
    fallback: Color = Color.Transparent
): Painter = rememberThumbHashPainter(
    thumbHash = rememberThumbHash(thumbHash),
    fallback = fallback
)

/**
 * Remembers a [Painter] that renders the already decoded [thumbHash].
 *
 * @param thumbHash the decoded [ThumbHash], or `null` to draw [fallback].
 * @param fallback color drawn when [thumbHash] is `null`.
 * @param intrinsicSize the painter's intrinsic size; defaults to the hash's reference size, 32 on the long side with the
 *   encoded aspect ratio, so that [ContentScale] crops or fits it like the real image. Pass [Size.Unspecified] to leave it unsized.
 */
@ExperimentalKaitekiApi
@Composable
public fun rememberThumbHashPainter(
    thumbHash: ThumbHash?,
    fallback: Color = Color.Transparent,
    intrinsicSize: Size = thumbHash.referenceSize
): Painter = if (thumbHash == null) {
    remember(fallback) { ColorPainter(fallback) }
} else {
    remember(thumbHash, intrinsicSize) { PlaceholderBitmapPainter(thumbHash.toImageBitmap(), intrinsicSize) }
}

/**
 * Draws the decoded [thumbHash] behind the content, filling the layout bounds.
 *
 * @param thumbHash the Base64 encoded [ThumbHash](https://evanw.github.io/thumbhash/).
 * @param fallback color drawn when [thumbHash] is malformed.
 */
@ExperimentalKaitekiApi
public fun Modifier.thumbHash(
    thumbHash: String,
    fallback: Color = Color.Transparent
): Modifier = this then ThumbHashElement(
    source = ThumbHashSource.Base64(thumbHash),
    fallback = fallback
)

/**
 * Draws the decoded [thumbHash] behind the content, filling the layout bounds.
 *
 * @param thumbHash the raw [ThumbHash](https://evanw.github.io/thumbhash/) bytes.
 * @param fallback color drawn when [thumbHash] is malformed.
 */
@ExperimentalKaitekiApi
public fun Modifier.thumbHash(
    thumbHash: ByteArray,
    fallback: Color = Color.Transparent
): Modifier = this then ThumbHashElement(
    source = ThumbHashSource.Bytes(thumbHash.copyOf()),
    fallback = fallback
)

@OptIn(ExperimentalKaitekiApi::class)
internal val ThumbHash?.referenceSize: Size
    get() = if (this == null) Size.Unspecified else Size(width.toFloat(), height.toFloat())

@OptIn(ExperimentalKaitekiApi::class)
private sealed interface ThumbHashSource {
    fun decode(): ThumbHash?

    data class Base64(val value: String) : ThumbHashSource {
        override fun decode(): ThumbHash? = ThumbHash.decode(value)
    }

    class Bytes(val value: ByteArray) : ThumbHashSource {
        override fun decode(): ThumbHash? = ThumbHash.decode(value)
        override fun equals(other: Any?): Boolean = other is Bytes && value.contentEquals(other.value)
        override fun hashCode(): Int = value.contentHashCode()
    }
}

private data class ThumbHashElement(
    val source: ThumbHashSource,
    val fallback: Color
) : ModifierNodeElement<ThumbHashNode>() {

    override fun create() = ThumbHashNode(source, fallback)

    override fun update(node: ThumbHashNode) {
        node.update(source, fallback)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "thumbHash"
        properties["thumbHash"] = when (source) {
            is ThumbHashSource.Base64 -> source.value
            is ThumbHashSource.Bytes -> source.value
        }
        properties["fallback"] = fallback
    }
}

private class ThumbHashNode(
    private var source: ThumbHashSource,
    private var fallback: Color
) : Modifier.Node(), DrawModifierNode {

    private var painter: Painter? = createPainter(source)

    fun update(
        source: ThumbHashSource,
        fallback: Color
    ) {
        var invalidate = false
        if (this.source != source) {
            this.source = source
            painter = createPainter(source)
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

@OptIn(ExperimentalKaitekiApi::class)
private fun createPainter(source: ThumbHashSource): Painter? {
    val decoded = source.decode() ?: return null
    return PlaceholderBitmapPainter(decoded.toImageBitmap(), decoded.referenceSize)
}

internal const val ThumbHashLuminanceGrid = 7

private inline fun forEachTriangle(nx: Int, ny: Int, block: (index: Int, cx: Int, cy: Int) -> Unit) {
    var index = 0
    for (cy in 0..<ny) {
        var cx = 0
        while (cx * ny < nx * (ny - cy)) {
            block(index++, cx, cy)
            cx++
        }
    }
}

/** Decodes the reference rendering ([ThumbHash.width] by [ThumbHash.height]) as non-premultiplied ARGB pixels, row by row. */
@OptIn(ExperimentalKaitekiApi::class)
internal fun ThumbHash.toArgbPixels(): IntArray {
    val pixels = IntArray(width * height)
    val alphaSize = if (hasAlpha) 5 else 1
    val fx = DoubleArray(ThumbHashLuminanceGrid)
    val fy = DoubleArray(ThumbHashLuminanceGrid)
    fun sum(coefficients: FloatArray, nx: Int, ny: Int): Double {
        var value = 0.0
        forEachTriangle(nx, ny) { index, cx, cy -> value += coefficients[index] * fx[cx] * fy[cy] }
        return value
    }
    for (y in 0..<height) {
        for (c in fy.indices) fy[c] = cos(PI / height * (y + 0.5) * c)
        for (x in 0..<width) {
            for (c in fx.indices) fx[c] = cos(PI / width * (x + 0.5) * c)
            val l = sum(lCoefficients, lx, ly)
            val p = sum(pCoefficients, 3, 3)
            val q = sum(qCoefficients, 3, 3)
            val a = sum(aCoefficients, alphaSize, alphaSize)
            val b = l - 2.0 / 3.0 * p
            val r = (3 * l - b + q) / 2
            val g = r - q
            pixels[y * width + x] = (channel(a) shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
        }
    }
    return pixels
}

private fun channel(value: Double): Int = (255 * value.coerceIn(0.0, 1.0)).toInt()

@OptIn(ExperimentalKaitekiApi::class)
internal fun ThumbHash.toImageBitmap(): ImageBitmap = createImageBitmap(toArgbPixels(), width, height)

private const val PreviewLandscape = "3OcRJYB4d3h/iIeHeEh3eIhw+j3A"
private const val PreviewPortrait = "1QcSHQRnh493V4dIh4eXh1h4kJUI"
private const val PreviewAlphaPortrait = "YJqGPQw7sFlslqhFafSE+Q6oJ1h2iHB2Rw=="
private const val PreviewAlphaSquare = "mYqDBQQnxnj0JoLYdN7f8JhpuDeHiHdwZw=="

@OptIn(ExperimentalKaitekiApi::class)
@Preview
@Composable
private fun ThumbHashModifierPreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.safeDrawingPadding()
    ) {
        Box(
            Modifier
                .width(224.dp)
                .aspectRatio(7f / 5f)
                .thumbHash(PreviewLandscape)
        )
        Box(
            Modifier
                .width(115.dp)
                .aspectRatio(5f / 7f)
                .thumbHash(PreviewPortrait)
        )
    }
}

@OptIn(ExperimentalKaitekiApi::class)
@Preview
@Composable
private fun ThumbHashContentScalePreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.safeDrawingPadding()
    ) {
        listOf(PreviewLandscape, PreviewPortrait).forEach { hash ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Crop" to ContentScale.Crop, "Fit" to ContentScale.Fit).forEach { (title, contentScale) ->
                    Box {
                        Image(
                            painter = rememberThumbHashPainter(hash),
                            contentDescription = null,
                            contentScale = contentScale,
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color.DarkGray)
                        )
                        Text(
                            text = title,
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalKaitekiApi::class)
@Preview
@Composable
private fun ThumbHashAlphaPreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.safeDrawingPadding()
    ) {
        listOf(PreviewAlphaPortrait, PreviewAlphaSquare).forEach { hash ->
            Image(
                painter = rememberThumbHashPainter(hash),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .checkerboard()
            )
        }
        Box(
            Modifier
                .size(120.dp)
                .checkerboard()
                .thumbHash(PreviewAlphaSquare)
        )
    }
}
