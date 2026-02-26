package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.InternalKaitekiApi
import com.kroegerama.kmp.kaiteki.dayDistanceTo
import com.vanniktech.locale.Language
import com.vanniktech.locale.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant

public enum class FormatStyle { SHORT, MEDIUM, LONG, FULL }
public enum class Direction { LAST_2, LAST, THIS, NEXT, NEXT_2, PLAIN }
public enum class AbsoluteUnit { DAY, MONTH, YEAR, NOW }
public enum class RelativeUnit { SECONDS, MINUTES, HOURS, DAYS, MONTHS, YEARS }
public enum class CapitalizationMode { NONE, MIDDLE_OF_SENTENCE, BEGINNING_OF_SENTENCE, UI_LIST_OR_MENU, STANDALONE }

@Stable
public interface LocalizedDateTimeFormatter {
    public val locale: Locale

    @Stable
    public fun formatDate(instant: Instant): String

    @Stable
    public fun formatDate(localDate: LocalDate): String

    @Stable
    public fun formatDate(localDateTime: LocalDateTime): String

    @Stable
    public fun formatTime(instant: Instant): String

    @Stable
    public fun formatTime(localTime: LocalTime): String

    @Stable
    public fun formatTime(localDateTime: LocalDateTime): String

    @Stable
    public fun formatDateTime(instant: Instant): String

    @Stable
    public fun formatDateTime(localDateTime: LocalDateTime): String

    @Stable
    public fun formatRelative(direction: Direction, unit: AbsoluteUnit): String?

    @Stable
    public fun formatRelative(quantity: Double, direction: Direction, unit: RelativeUnit): String?

    @OptIn(InternalKaitekiApi::class)
    @FrequentlyChangingValue
    public fun formatFancy(
        instant: Instant,
        dateTimeDivider: (Locale) -> String = ::defaultTimeDivider,
        switchNowToSeconds: Long = DEFAULT_SWITCH_NOW_TO_SEC,
        switchSecondsToMinutes: Long = DEFAULT_SWITCH_SEC_TO_MIN,
        switchMinutesToTime: Long = DEFAULT_SWITCH_MIN_TO_TIME
    ): String = formatFancyInternal(
        instant = instant,
        dateTimeDivider = dateTimeDivider,
        switchNowToSeconds = switchNowToSeconds,
        switchSecondsToMinutes = switchSecondsToMinutes,
        switchMinutesToTime = switchMinutesToTime
    ).first

    public companion object {
        public fun defaultTimeDivider(locale: Locale): String = when (locale.language) {
            Language.CHINESE,
            Language.JAPANESE,
            Language.KOREAN,
            Language.THAI -> " "

            Language.ARABIC,
            Language.FARSI,
            Language.URDU -> "، "

            else -> ", "
        }

        public const val DEFAULT_SWITCH_NOW_TO_SEC: Long = 3L
        public const val DEFAULT_SWITCH_SEC_TO_MIN: Long = 60L * 2L
        public const val DEFAULT_SWITCH_MIN_TO_TIME: Long = 60L * 15L
    }
}

@InternalKaitekiApi
@FrequentlyChangingValue
public fun LocalizedDateTimeFormatter.formatFancyInternal(
    instant: Instant,
    dateTimeDivider: (Locale) -> String,
    switchNowToSeconds: Long,
    switchSecondsToMinutes: Long,
    switchMinutesToTime: Long
): Pair<String, Long> {
    val now = Clock.System.now()
    val dayOffset = instant.dayDistanceTo(now)
    val millisOffset = instant.toEpochMilliseconds() - now.toEpochMilliseconds()
    val past = millisOffset < 0
    val secondsOffsetAbsolute = (millisOffset / 1000).absoluteValue

    if (secondsOffsetAbsolute < switchNowToSeconds) {
        val nowPlain = formatRelative(Direction.PLAIN, AbsoluteUnit.NOW) ?: formatDateTime(instant)
        return nowPlain to 1000
    }

    val date: String? = when (dayOffset) {
        -2L -> formatRelative(Direction.LAST_2, AbsoluteUnit.DAY) ?: formatDate(instant)
        -1L -> formatRelative(Direction.LAST, AbsoluteUnit.DAY) ?: formatDate(instant)

        0L -> if (secondsOffsetAbsolute < switchMinutesToTime) {
            null
        } else {
            formatRelative(Direction.THIS, AbsoluteUnit.DAY)
        }

        1L -> formatRelative(Direction.NEXT, AbsoluteUnit.DAY) ?: formatDate(instant)
        2L -> formatRelative(Direction.NEXT_2, AbsoluteUnit.DAY) ?: formatDate(instant)
        else -> return formatDateTime(instant) to 30_000L
    }

    val time = if (dayOffset == 0L) {
        val direction: Direction = if (past) {
            Direction.LAST
        } else {
            Direction.NEXT
        }

        when {
            secondsOffsetAbsolute < switchSecondsToMinutes -> formatRelative(
                secondsOffsetAbsolute.toDouble(),
                direction,
                RelativeUnit.SECONDS
            ) ?: return formatDateTime(instant) to 30_000L

            secondsOffsetAbsolute < switchMinutesToTime -> formatRelative(
                secondsOffsetAbsolute.div(60).toDouble(),
                direction,
                RelativeUnit.MINUTES
            ) ?: return formatDateTime(instant) to 30_000L

            else -> formatTime(instant)
        }
    } else {
        formatTime(instant)
    }

    val str = listOfNotNull(date, time).joinToString(dateTimeDivider(locale))
    val delay = when {
        secondsOffsetAbsolute < switchSecondsToMinutes -> 1_000L
        secondsOffsetAbsolute < switchMinutesToTime -> 10_000L
        else -> 30_000L
    }
    return str to delay
}

@RememberInComposition
public expect fun defaultLocalizedDateTimeFormatter(
    locale: Locale,
    dateStyle: FormatStyle = FormatStyle.MEDIUM,
    timeStyle: FormatStyle = FormatStyle.SHORT,
    capitalizationMode: CapitalizationMode = CapitalizationMode.NONE,
    zone: TimeZone = TimeZone.currentSystemDefault()
): LocalizedDateTimeFormatter
