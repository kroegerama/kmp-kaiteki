package com.kroegerama.kmp.kaiteki.compose.feature

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun createImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int
): ImageBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
