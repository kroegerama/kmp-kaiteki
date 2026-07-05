package com.kroegerama.kmp.kaiteki.compose.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import platform.UIKit.UIColor

public fun Color.asUIColor(): UIColor {
    val srgb = convert(ColorSpaces.Srgb)
    return UIColor(
        red = srgb.red.toDouble(),
        green = srgb.green.toDouble(),
        blue = srgb.blue.toDouble(),
        alpha = srgb.alpha.toDouble()
    )
}
