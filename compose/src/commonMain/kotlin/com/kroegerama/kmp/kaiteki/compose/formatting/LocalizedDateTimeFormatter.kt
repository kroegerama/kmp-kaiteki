package com.kroegerama.kmp.kaiteki.compose.formatting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.kroegerama.kmp.kaiteki.InternalKaitekiApi
import com.kroegerama.kmp.kaiteki.formatting.CapitalizationMode
import com.kroegerama.kmp.kaiteki.formatting.FormatStyle
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter.Companion.DEFAULT_SWITCH_MIN_TO_TIME
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter.Companion.DEFAULT_SWITCH_NOW_TO_SEC
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter.Companion.DEFAULT_SWITCH_SEC_TO_MIN
import com.kroegerama.kmp.kaiteki.formatting.defaultLocalizedDateTimeFormatter
import com.kroegerama.kmp.kaiteki.formatting.formatFancyInternal
import com.vanniktech.locale.Country
import com.vanniktech.locale.Language
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import com.vanniktech.locale.Locale as KMPLocale

@Composable
public fun rememberLocalizedDateTimeFormatter(
    locale: ComposeLocale = ComposeLocale.current,
    dateStyle: FormatStyle = FormatStyle.MEDIUM,
    timeStyle: FormatStyle = FormatStyle.SHORT,
    capitalizationMode: CapitalizationMode = CapitalizationMode.NONE,
    zone: TimeZone = TimeZone.currentSystemDefault()
): LocalizedDateTimeFormatter = remember(locale, dateStyle, timeStyle, capitalizationMode, zone) {
    defaultLocalizedDateTimeFormatter(
        composeLocale = locale,
        dateStyle = dateStyle,
        timeStyle = timeStyle,
        capitalizationMode = capitalizationMode,
        zone = zone
    )
}

@RememberInComposition
public fun defaultLocalizedDateTimeFormatter(
    composeLocale: ComposeLocale,
    dateStyle: FormatStyle = FormatStyle.MEDIUM,
    timeStyle: FormatStyle = FormatStyle.SHORT,
    capitalizationMode: CapitalizationMode = CapitalizationMode.NONE,
    zone: TimeZone = TimeZone.currentSystemDefault()
): LocalizedDateTimeFormatter = defaultLocalizedDateTimeFormatter(
    locale = KMPLocale.fromOrNull(composeLocale.toString()) ?: KMPLocale(Language.ENGLISH, Country.USA)
)

public val LocalLocalizedDateTimeFormatter: ProvidableCompositionLocal<LocalizedDateTimeFormatter> = compositionLocalOf {
    defaultLocalizedDateTimeFormatter(
        locale = KMPLocale.from(ComposeLocale.current.toString())
    )
}

@OptIn(InternalKaitekiApi::class)
@Composable
public fun LocalizedDateTimeFormatter.formatFancyAsState(
    instant: Instant,
    dateTimeDivider: (KMPLocale) -> String = LocalizedDateTimeFormatter::defaultTimeDivider,
    switchNowToSeconds: Long = DEFAULT_SWITCH_NOW_TO_SEC,
    switchSecondsToMinutes: Long = DEFAULT_SWITCH_SEC_TO_MIN,
    switchMinutesToTime: Long = DEFAULT_SWITCH_MIN_TO_TIME
): State<String> = produceState(
    initialValue = "",
    this,
    instant,
    dateTimeDivider,
    switchNowToSeconds,
    switchSecondsToMinutes,
    switchMinutesToTime
) {
    while (isActive) {
        val (formatted, delay) = formatFancyInternal(
            instant = instant,
            dateTimeDivider = dateTimeDivider,
            switchNowToSeconds = switchNowToSeconds,
            switchSecondsToMinutes = switchSecondsToMinutes,
            switchMinutesToTime = switchMinutesToTime
        )
        value = formatted
        delay(delay)
    }
}
