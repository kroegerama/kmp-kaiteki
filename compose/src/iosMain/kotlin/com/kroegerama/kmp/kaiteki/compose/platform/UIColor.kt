package com.kroegerama.kmp.kaiteki.compose.platform

import androidx.compose.ui.graphics.Color
import platform.UIKit.UIColor

public fun Color.asUIColor(): UIColor = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble()
)
