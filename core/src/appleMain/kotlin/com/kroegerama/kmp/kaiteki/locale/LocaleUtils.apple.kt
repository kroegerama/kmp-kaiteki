package com.kroegerama.kmp.kaiteki.locale

import androidx.compose.runtime.Stable
import platform.Foundation.countryCode

public actual val PlatformLocale.unicodeFlag: String
    @Stable
    get() = isoCountryCodeToFlag(countryCode ?: "")
