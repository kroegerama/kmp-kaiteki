package com.kroegerama.kmp.kaiteki.compose.formatting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize.Companion.Auto
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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
    initialValue = remember {
        formatFancyInternal(
            instant = instant,
            dateTimeDivider = dateTimeDivider,
            switchNowToSeconds = switchNowToSeconds,
            switchSecondsToMinutes = switchSecondsToMinutes,
            switchMinutesToTime = switchMinutesToTime
        ).first
    },
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

@OptIn(ExperimentalGridApi::class)
@Preview(locale = "en")
@Preview(locale = "de")
@Preview(locale = "fr")
@Preview(locale = "ar")
@Preview(locale = "fa")
@Composable
private fun FancyDateTimeFormatterPreview() {
    MaterialTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                val formatter = rememberLocalizedDateTimeFormatter()
                val now = remember { Clock.System.now() }
                val dates = remember(now) {
                    listOf(
                        now.minus(3.days),
                        now.minus(2.days),
                        now.minus(1.days),
                        now.minus(1.hours),
                        now.minus(5.minutes),
                        now.minus(30.seconds),
                        now,
                        now.plus(30.seconds),
                        now.plus(5.minutes),
                        now.plus(1.hours),
                        now.plus(1.days),
                        now.plus(2.days),
                        now.plus(3.days),
                    )
                }

                dates.forEach {
                    val formatted by formatter.formatFancyAsState(it)
                    Text(text = formatted)
                }

                HorizontalDivider()

                Grid(
                    {
                        gap(8.dp)
                        columns(Auto, Auto)
                    }
                ) {
                    Text("formatDate")
                    Text(formatter.formatDate(now))
                    Text("formatTime")
                    Text(formatter.formatTime(now))
                    Text("formatDateTime")
                    Text(formatter.formatDateTime(now))
                }
            }
        }
    }
}
