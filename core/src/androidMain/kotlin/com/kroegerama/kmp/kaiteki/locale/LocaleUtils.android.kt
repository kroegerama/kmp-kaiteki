package com.kroegerama.kmp.kaiteki.locale

import androidx.compose.runtime.Stable

public actual val PlatformLocale.unicodeFlag: String
    @Stable
    get() = isoCountryCodeToFlag(country)
