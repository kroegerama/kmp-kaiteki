package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Stable
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import com.kroegerama.kmp.kaiteki.locale.languageTag
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName
import kotlin.native.concurrent.ThreadLocal

@Stable
public actual fun Month.displayName(
    style: TextStyle,
    locale: PlatformLocale
): String {
    val formatter = DisplayNameUtils.formatter(locale)
    val symbols = when (style) {
        TextStyle.FULL -> formatter.monthSymbols
        TextStyle.FULL_STANDALONE -> formatter.standaloneMonthSymbols
        TextStyle.SHORT -> formatter.shortMonthSymbols
        TextStyle.SHORT_STANDALONE -> formatter.shortStandaloneMonthSymbols
        TextStyle.NARROW -> formatter.veryShortMonthSymbols
        TextStyle.NARROW_STANDALONE -> formatter.veryShortStandaloneMonthSymbols
    }
    return symbols[ordinal] as? String ?: name
}

@Stable
public actual fun DayOfWeek.displayName(
    style: TextStyle,
    locale: PlatformLocale
): String {
    val formatter = DisplayNameUtils.formatter(locale)
    val symbols = when (style) {
        TextStyle.FULL -> formatter.weekdaySymbols
        TextStyle.FULL_STANDALONE -> formatter.standaloneWeekdaySymbols
        TextStyle.SHORT -> formatter.shortWeekdaySymbols
        TextStyle.SHORT_STANDALONE -> formatter.shortStandaloneWeekdaySymbols
        TextStyle.NARROW -> formatter.veryShortWeekdaySymbols
        TextStyle.NARROW_STANDALONE -> formatter.veryShortStandaloneWeekdaySymbols
    }
    // NSDateFormatter weekday symbols start with Sunday (index 0); DayOfWeek starts with Monday (ordinal 0).
    return symbols[(ordinal + 1) % 7] as? String ?: name
}

@Stable
public actual fun YearMonth.displayName(
    style: TextStyle,
    locale: PlatformLocale
): String {
    val formatter = DisplayNameUtils.yearMonthFormatter(locale, style)
    val components = NSDateComponents().also {
        it.calendar = DisplayNameUtils.calendar
        it.timeZone = DisplayNameUtils.utc
        it.year = year.toLong()
        it.month = month.ordinal + 1L
        it.day = 1
    }
    val date = DisplayNameUtils.calendar.dateFromComponents(components) ?: return toString()
    return formatter.stringFromDate(date)
}

/**
 * Per-thread cache of the configured Foundation formatters and the immutable symbol arrays.
 * `@ThreadLocal` gives every thread its own instances, which both avoids the cost of rebuilding
 * `NSDateFormatter`s and side-steps their lack of thread-safety during configuration.
 */
@ThreadLocal
private object DisplayNameUtils {
    val calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)!!
    val utc = NSTimeZone.timeZoneWithName("UTC")!!

    private val formatters = HashMap<String, NSDateFormatter>()

    fun formatter(locale: PlatformLocale) = formatters.getOrPut(locale.languageTag) {
        NSDateFormatter().apply {
            this.locale = locale
            this.calendar = this@DisplayNameUtils.calendar
        }
    }

    private val yearMonthFormatters = HashMap<String, NSDateFormatter>()

    private fun key(locale: PlatformLocale, style: TextStyle) = "${locale.languageTag}|${style.name}"

    fun yearMonthFormatter(locale: PlatformLocale, style: TextStyle): NSDateFormatter = yearMonthFormatters.getOrPut(key(locale, style)) {
        val skeleton = style.toYearMonthSkeleton()
        NSDateFormatter().apply {
            this.locale = locale
            this.calendar = this@DisplayNameUtils.calendar
            // The formatted NSDate is built at midnight UTC, so format in UTC as well —
            // otherwise the month/year shift for zones west of UTC.
            this.timeZone = this@DisplayNameUtils.utc
            // Must run after locale + calendar are set so the picked pattern matches them.
            setLocalizedDateFormatFromTemplate(skeleton)
        }
    }
}
