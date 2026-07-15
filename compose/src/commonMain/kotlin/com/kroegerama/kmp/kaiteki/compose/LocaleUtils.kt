package com.kroegerama.kmp.kaiteki.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.intl.Locale
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale

/** Converts this Compose [Locale] into a [PlatformLocale] for use with the Kaiteki formatting APIs. */
public expect fun Locale.asPlatformLocale(): PlatformLocale

/** [CompositionLocal] providing the current [LocalLocale] as a [PlatformLocale]. */
public val LocalPlatformLocale: ProvidableCompositionLocal<PlatformLocale> = compositionLocalWithComputedDefaultOf {
    LocalLocale.currentValue.asPlatformLocale()
}
