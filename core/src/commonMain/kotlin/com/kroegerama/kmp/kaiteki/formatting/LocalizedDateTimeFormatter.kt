package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.InternalKaitekiApi
import com.kroegerama.kmp.kaiteki.datetime.dayDistanceTo
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import com.kroegerama.kmp.kaiteki.locale.language
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Locale- and timezone-aware formatter for dates, times and relative phrases.
 *
 * All `format*` methods render according to [locale] and [zone]. Obtain an instance via
 * [defaultLocalizedDateTimeFormatter].
 */
@Stable
public interface LocalizedDateTimeFormatter {
    /** Locale used for translation and formatting patterns. */
    public val locale: PlatformLocale

    /** Time zone used when converting an [Instant] to a wall-clock date or time. */
    public val zone: TimeZone

    /** Formats the date part of [instant] (in [zone]). */
    @Stable
    public fun formatDate(instant: Instant): String

    /** Formats [localDate]. */
    @Stable
    public fun formatDate(localDate: LocalDate): String

    /** Formats the date part of [localDateTime]. */
    @Stable
    public fun formatDate(localDateTime: LocalDateTime): String

    /** Formats the time part of [instant] (in [zone]). */
    @Stable
    public fun formatTime(instant: Instant): String

    /** Formats [localTime]. */
    @Stable
    public fun formatTime(localTime: LocalTime): String

    /** Formats the time part of [localDateTime]. */
    @Stable
    public fun formatTime(localDateTime: LocalDateTime): String

    /** Formats both the date and time parts of [instant] (in [zone]). */
    @Stable
    public fun formatDateTime(instant: Instant): String

    /** Formats both the date and time parts of [localDateTime]. */
    @Stable
    public fun formatDateTime(localDateTime: LocalDateTime): String

    /**
     * Localized relative phrase for a whole unit, e.g. "yesterday", "next month".
     *
     * @return the phrase, or `null` if the locale has no dedicated wording for this combination.
     */
    @Stable
    public fun formatRelative(direction: Direction, unit: AbsoluteUnit): String?

    /**
     * Localized relative phrase with a quantity, e.g. "3 minutes ago", "in 2 days".
     *
     * @param quantity magnitude of the offset in [unit].
     * @return the phrase, or `null` if the locale has no dedicated wording for this combination.
     */
    @Stable
    public fun formatRelative(quantity: Double, direction: Direction, unit: RelativeUnit): String?

    /**
     * Formats [instant] as a compact, human-friendly string whose precision adapts to how far away
     * it is: "now", then seconds, then minutes, then a time, then a relative day plus time, and
     * finally an absolute date-time for anything further out.
     *
     * @param dateTimeDivider produces the separator inserted between the date and time parts.
     * @param switchNowToSeconds offsets below this many seconds are shown as "now".
     * @param switchSecondsToMinutes offsets below this many seconds are shown in seconds.
     * @param switchMinutesToTime offsets below this many seconds are shown in minutes; larger
     *   same-day offsets fall back to a formatted time.
     */
    @OptIn(InternalKaitekiApi::class)
    @FrequentlyChangingValue
    public fun formatFancy(
        instant: Instant,
        dateTimeDivider: (PlatformLocale) -> String = ::defaultTimeDivider,
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
        /** Locale-appropriate separator between date and time, e.g. `", "` for most languages. */
        public fun defaultTimeDivider(locale: PlatformLocale): String = when (locale.language) {
            "zh",
            "ja",
            "ko",
            "th" -> " "

            "ar",
            "fa",
            "ur" -> "، "

            else -> ", "
        }

        /** Default for [formatFancy]'s `switchNowToSeconds`: 3 seconds. */
        public const val DEFAULT_SWITCH_NOW_TO_SEC: Long = 3L

        /** Default for [formatFancy]'s `switchSecondsToMinutes`: 2 minutes. */
        public const val DEFAULT_SWITCH_SEC_TO_MIN: Long = 60L * 2L

        /** Default for [formatFancy]'s `switchMinutesToTime`: 15 minutes. */
        public const val DEFAULT_SWITCH_MIN_TO_TIME: Long = 60L * 15L
    }
}

/**
 * Backing implementation of [LocalizedDateTimeFormatter.formatFancy] that also returns the suggested
 * delay after which the result should be recomputed. Intended for driving a self-updating clock.
 *
 * @return the formatted string paired with the [Duration] until it may next change.
 */
@InternalKaitekiApi
@FrequentlyChangingValue
public fun LocalizedDateTimeFormatter.formatFancyInternal(
    instant: Instant,
    dateTimeDivider: (PlatformLocale) -> String,
    switchNowToSeconds: Long,
    switchSecondsToMinutes: Long,
    switchMinutesToTime: Long,
    clock: Clock = Clock.System
): Pair<String, Duration> {
    val now = clock.now()
    val dayOffset = instant.dayDistanceTo(now, zone)
    val millisOffset = instant.toEpochMilliseconds() - now.toEpochMilliseconds()
    val past = millisOffset < 0
    val secondsOffsetAbsolute = (millisOffset / 1000).absoluteValue

    if (secondsOffsetAbsolute < switchNowToSeconds) {
        val nowPlain = formatRelative(Direction.PLAIN, AbsoluteUnit.NOW) ?: formatDateTime(instant)
        return nowPlain to 1.seconds
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
        else -> return formatDateTime(instant) to 30.seconds
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
            ) ?: return formatDateTime(instant) to 30.seconds

            secondsOffsetAbsolute < switchMinutesToTime -> formatRelative(
                secondsOffsetAbsolute.div(60).toDouble(),
                direction,
                RelativeUnit.MINUTES
            ) ?: return formatDateTime(instant) to 30.seconds

            else -> formatTime(instant)
        }
    } else {
        formatTime(instant)
    }

    val str = listOfNotNull(date, time).joinToString(dateTimeDivider(locale))
    val delay = when {
        secondsOffsetAbsolute < switchSecondsToMinutes -> 1.seconds
        secondsOffsetAbsolute < switchMinutesToTime -> 10.seconds
        else -> 30.seconds
    }
    return str to delay
}

/**
 * Creates the default platform-backed [LocalizedDateTimeFormatter].
 *
 * @param locale locale for translation and patterns.
 * @param dateStyle length of formatted dates.
 * @param timeStyle length of formatted times.
 * @param capitalizationMode how localized names are capitalized.
 * @param zone time zone applied when formatting an [Instant].
 */
@RememberInComposition
public expect fun defaultLocalizedDateTimeFormatter(
    locale: PlatformLocale,
    dateStyle: FormatStyle = FormatStyle.MEDIUM,
    timeStyle: FormatStyle = FormatStyle.SHORT,
    capitalizationMode: CapitalizationMode = CapitalizationMode.NONE,
    zone: TimeZone = TimeZone.currentSystemDefault()
): LocalizedDateTimeFormatter
