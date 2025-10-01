package com.kroegerama.kmp.kaiteki.compose.formatting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import com.kroegerama.kmp.kaiteki.formatting.DecimalFormatter
import com.kroegerama.kmp.kaiteki.formatting.DefaultDecimalFormatter
import com.vanniktech.locale.Country
import com.vanniktech.locale.Language
import com.vanniktech.locale.Locale as KMPLocale

@Composable
public fun rememberDecimalFormatter(
    locale: Locale = Locale.current,
    minimumFractionDigits: Int = 0,
    minimumIntegerDigits: Int = 1,
    maximumFractionDigits: Int = 3,
    maximumIntegerDigits: Int = 40,
    isGroupingUsed: Boolean = true
): DecimalFormatter = remember(
    locale,
    minimumFractionDigits,
    minimumIntegerDigits,
    maximumFractionDigits,
    minimumFractionDigits,
    isGroupingUsed
) {
    DefaultDecimalFormatter(
        locale = KMPLocale.fromOrNull(locale.toString()) ?: KMPLocale(Language.ENGLISH, Country.USA),
        minimumFractionDigits = minimumFractionDigits,
        minimumIntegerDigits = minimumIntegerDigits,
        maximumFractionDigits = maximumFractionDigits,
        maximumIntegerDigits = maximumIntegerDigits,
        isGroupingUsed = isGroupingUsed,
    )
}

public val LocalDecimalFormatter: ProvidableCompositionLocal<DecimalFormatter> = compositionLocalOf {
    DefaultDecimalFormatter(
        locale = KMPLocale.from(Locale.current.toString())
    )
}
