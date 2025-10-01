package com.kroegerama.kmp.kaiteki.formatting

import java.util.Locale

internal actual fun formatHumanReadableBytes(value: Float, pre: String): String =
    "%.1f %sB".format(Locale.US, value, pre)
