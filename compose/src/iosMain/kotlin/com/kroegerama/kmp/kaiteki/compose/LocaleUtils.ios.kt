package com.kroegerama.kmp.kaiteki.compose

import androidx.compose.ui.text.intl.Locale
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import platform.Foundation.NSLocale

public actual fun Locale.asPlatformLocale(): PlatformLocale = NSLocale(toLanguageTag())
