package com.kroegerama.kmp.kaiteki

import androidx.compose.runtime.Stable
import java.util.Locale

public val Locale.unicodeFlag: String
    @Stable
    get() = isoCountryCodeToFlag(country)
