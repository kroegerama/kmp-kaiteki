package com.kroegerama.kmp.kaiteki.compose

import androidx.compose.ui.text.intl.Locale
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale

public actual fun Locale.asPlatformLocale(): PlatformLocale = platformLocale
