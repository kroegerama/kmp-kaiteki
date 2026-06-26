package com.kroegerama.kmp.kaiteki.formatting

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

internal actual fun formatHumanReadableBytes(value: Float, pre: String): String =
    NSString.stringWithFormat("%.1f", value) + " ${pre}B"
