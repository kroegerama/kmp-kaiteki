package com.kroegerama.kmp.kaiteki.compose

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.intl.Locale
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale

public expect fun Locale.asPlatformLocale(): PlatformLocale

public val LocalPlatformLocale: CompositionLocal<PlatformLocale> = compositionLocalWithComputedDefaultOf {
    LocalLocale.currentValue.asPlatformLocale()
}
