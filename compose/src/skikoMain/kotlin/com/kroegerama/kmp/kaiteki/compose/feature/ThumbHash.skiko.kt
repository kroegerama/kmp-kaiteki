package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual fun createImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int
): ImageBitmap {
    val bytes = ByteArray(pixels.size * 4)
    pixels.forEachIndexed { index, argb ->
        val offset = index * 4
        bytes[offset] = argb.toByte()
        bytes[offset + 1] = (argb shr 8).toByte()
        bytes[offset + 2] = (argb shr 16).toByte()
        bytes[offset + 3] = (argb ushr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)
    val bitmap = Bitmap()
    bitmap.installPixels(info, bytes, width * 4)
    return bitmap.asComposeImageBitmap()
}
