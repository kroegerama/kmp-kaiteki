package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.InternalKaitekiApi
import com.kroegerama.kmp.kaiteki.dayDistanceTo
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

@Immutable
public interface LocalizedDateTimeFormatter {
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
        dateTimeDivider: String = DEFAULT_TIME_DIVIDER,
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
        public const val DEFAULT_TIME_DIVIDER: String = ", "
        public const val DEFAULT_SWITCH_NOW_TO_SEC: Long = 3L
        public const val DEFAULT_SWITCH_SEC_TO_MIN: Long = 60L * 2L
        public const val DEFAULT_SWITCH_MIN_TO_TIME: Long = 60L * 15L
    }
}

@InternalKaitekiApi
@FrequentlyChangingValue
public fun LocalizedDateTimeFormatter.formatFancyInternal(
    instant: Instant,
    dateTimeDivider: String,
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
        else -> formatDate(instant)
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
            )

            secondsOffsetAbsolute < switchMinutesToTime -> formatRelative(
                secondsOffsetAbsolute.div(60).toDouble(),
                direction,
                RelativeUnit.MINUTES
            )

            else -> formatTime(instant)
        }
    } else {
        formatTime(instant)
    }

    val str = listOfNotNull(date, time).joinToString(dateTimeDivider)
    val delay = when {
        secondsOffsetAbsolute < switchSecondsToMinutes -> 1_000L
        secondsOffsetAbsolute < switchMinutesToTime -> 10_000L
        else -> 30_000L
    }
    return str to delay
}

public expect class DefaultLocalizedDateTimeFormatter
@RememberInComposition constructor(
    locale: Locale,
    dateStyle: FormatStyle = FormatStyle.MEDIUM,
    timeStyle: FormatStyle = FormatStyle.SHORT,
    zone: TimeZone = TimeZone.currentSystemDefault()
) : LocalizedDateTimeFormatter {
    override fun formatDate(instant: Instant): String
    override fun formatDate(localDate: LocalDate): String
    override fun formatDate(localDateTime: LocalDateTime): String

    override fun formatTime(instant: Instant): String
    override fun formatTime(localTime: LocalTime): String
    override fun formatTime(localDateTime: LocalDateTime): String

    override fun formatDateTime(instant: Instant): String
    override fun formatDateTime(localDateTime: LocalDateTime): String

    override fun formatRelative(direction: Direction, unit: AbsoluteUnit): String?
    override fun formatRelative(quantity: Double, direction: Direction, unit: RelativeUnit): String?
}
