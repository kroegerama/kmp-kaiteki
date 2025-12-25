package com.kroegerama.kmp.kaiteki.compose.formatting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import com.kroegerama.kmp.kaiteki.InternalKaitekiApi
import com.kroegerama.kmp.kaiteki.formatting.DefaultLocalizedDateTimeFormatter
import com.kroegerama.kmp.kaiteki.formatting.FormatStyle
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter.Companion.DEFAULT_SWITCH_MIN_TO_TIME
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter.Companion.DEFAULT_SWITCH_NOW_TO_SEC
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter.Companion.DEFAULT_SWITCH_SEC_TO_MIN
import com.kroegerama.kmp.kaiteki.formatting.LocalizedDateTimeFormatter.Companion.DEFAULT_TIME_DIVIDER
import com.kroegerama.kmp.kaiteki.formatting.formatFancyInternal
import com.vanniktech.locale.Country
import com.vanniktech.locale.Language
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
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

@OptIn(InternalKaitekiApi::class)
@Composable
public fun LocalizedDateTimeFormatter.formatFancyAsState(
    instant: Instant,
    dateTimeDivider: String = DEFAULT_TIME_DIVIDER,
    switchNowToSeconds: Long = DEFAULT_SWITCH_NOW_TO_SEC,
    switchSecondsToMinutes: Long = DEFAULT_SWITCH_SEC_TO_MIN,
    switchMinutesToTime: Long = DEFAULT_SWITCH_MIN_TO_TIME
): State<String> {
    val state = remember {
        mutableStateOf(
            formatFancyInternal(
                instant = instant,
                dateTimeDivider = dateTimeDivider,
                switchNowToSeconds = switchNowToSeconds,
                switchSecondsToMinutes = switchSecondsToMinutes,
                switchMinutesToTime = switchMinutesToTime
            ).first
        )
    }
    LaunchedEffect(
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
            state.value = formatted
            delay(delay)
        }
    }
    return state
}
