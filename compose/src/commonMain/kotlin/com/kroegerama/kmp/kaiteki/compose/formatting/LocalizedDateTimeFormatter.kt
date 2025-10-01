package com.kroegerama.kmp.kaiteki.compose.formatting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import com.kroegerama.kmp.kaiteki.formatting.DefaultLocalizedDateTimeFormatter
import com.kroegerama.kmp.kaiteki.formatting.FormatStyle
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter
import com.vanniktech.locale.Country
import com.vanniktech.locale.Language
import kotlinx.datetime.TimeZone
import com.vanniktech.locale.Locale as KMPLocale

@Composable
public fun rememberLocalizedDateTimeFormatter(
    locale: Locale = Locale.current,
    dateStyle: FormatStyle = FormatStyle.MEDIUM,
    timeStyle: FormatStyle = FormatStyle.SHORT,
    zone: TimeZone = TimeZone.currentSystemDefault()
): LocalizedDateTimeFormatter = remember(locale, dateStyle, timeStyle, zone) {
    DefaultLocalizedDateTimeFormatter(
        locale = KMPLocale.fromOrNull(locale.toString()) ?: KMPLocale(Language.ENGLISH, Country.USA),
        dateStyle = dateStyle,
        timeStyle = timeStyle,
        zone = zone
    )
}

public val LocalLocalizedDateTimeFormatter: ProvidableCompositionLocal<LocalizedDateTimeFormatter> = compositionLocalOf {
    DefaultLocalizedDateTimeFormatter(
        locale = KMPLocale.from(Locale.current.toString()),
        dateStyle = FormatStyle.MEDIUM,
        timeStyle = FormatStyle.SHORT,
        zone = TimeZone.currentSystemDefault()
    )
}
