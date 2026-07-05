package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toNSDate
import kotlinx.datetime.toNSDateComponents
import kotlinx.datetime.toNSTimeZone
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSFormattingContext
import platform.Foundation.NSFormattingContextBeginningOfSentence
import platform.Foundation.NSFormattingContextListItem
import platform.Foundation.NSFormattingContextMiddleOfSentence
import platform.Foundation.NSFormattingContextStandalone
import platform.Foundation.NSFormattingContextUnknown
import platform.Foundation.NSRelativeDateTimeFormatter
import platform.Foundation.NSRelativeDateTimeFormatterStyleNamed
import platform.Foundation.NSRelativeDateTimeFormatterUnitsStyleFull
import kotlin.time.Instant

@RememberInComposition
public actual fun defaultLocalizedDateTimeFormatter(
    locale: PlatformLocale,
    dateStyle: FormatStyle,
    timeStyle: FormatStyle,
    capitalizationMode: CapitalizationMode,
    zone: TimeZone
): LocalizedDateTimeFormatter = DefaultLocalizedDateTimeFormatter(
    locale = locale,
    dateStyle = dateStyle,
    timeStyle = timeStyle,
    capitalizationMode = capitalizationMode,
    zone = zone
)

internal class DefaultLocalizedDateTimeFormatter(
    override val locale: PlatformLocale,
    dateStyle: FormatStyle,
    timeStyle: FormatStyle,
    capitalizationMode: CapitalizationMode,
    override val zone: TimeZone
) : LocalizedDateTimeFormatter {

    private val dateFormatter: NSDateFormatter = NSDateFormatter().also {
        it.locale = locale
        it.timeZone = zone.toNSTimeZone()
        it.dateStyle = dateStyle.toNSDateFormatterStyle()
        it.timeStyle = NSDateFormatterNoStyle
    }
    private val timeFormatter: NSDateFormatter = NSDateFormatter().also {
        it.locale = locale
        it.timeZone = zone.toNSTimeZone()
        it.dateStyle = NSDateFormatterNoStyle
        it.timeStyle = timeStyle.toNSDateFormatterStyle()
    }
    private val dateTimeFormatter: NSDateFormatter = NSDateFormatter().also {
        it.locale = locale
        it.timeZone = zone.toNSTimeZone()
        it.dateStyle = dateStyle.toNSDateFormatterStyle()
        it.timeStyle = timeStyle.toNSDateFormatterStyle()
    }
    // pinned to Gregorian: kotlinx-datetime components are ISO; the user's current calendar
    // (e.g. Buddhist, Japanese) would reinterpret the year and shift dates by centuries
    private val calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)!!.also {
        it.setTimeZone(zone.toNSTimeZone())
    }
    private val relativeDateTimeFormatter = NSRelativeDateTimeFormatter().also {
        it.locale = locale
        it.unitsStyle = NSRelativeDateTimeFormatterUnitsStyleFull
        it.dateTimeStyle = NSRelativeDateTimeFormatterStyleNamed
        it.formattingContext = capitalizationMode.toNSFormattingContext()
    }

    override fun formatDate(instant: Instant): String = dateFormatter.stringFromDate(instant.toNSDate())
    override fun formatDate(localDate: LocalDate): String = dateFormatter.stringFromDate(localDate.toNSDate())
    override fun formatDate(localDateTime: LocalDateTime): String = dateFormatter.stringFromDate(localDateTime.toNSDate())

    override fun formatTime(instant: Instant): String = timeFormatter.stringFromDate(instant.toNSDate())
    override fun formatTime(localTime: LocalTime): String = timeFormatter.stringFromDate(localTime.toNSDate())
    override fun formatTime(localDateTime: LocalDateTime): String = timeFormatter.stringFromDate(localDateTime.toNSDate())

    override fun formatDateTime(instant: Instant): String = dateTimeFormatter.stringFromDate(instant.toNSDate())
    override fun formatDateTime(localDateTime: LocalDateTime): String = dateTimeFormatter.stringFromDate(localDateTime.toNSDate())

    override fun formatRelative(direction: Direction, unit: AbsoluteUnit): String? {
        val components = NSDateComponents().apply {
            when (unit) {
                AbsoluteUnit.NOW -> second = 0

                AbsoluteUnit.DAY -> day = when (direction) {
                    Direction.LAST -> -1
                    Direction.THIS, Direction.PLAIN -> 0
                    Direction.NEXT -> 1
                    else -> return null
                }

                AbsoluteUnit.MONTH -> month = when (direction) {
                    Direction.LAST -> -1
                    Direction.THIS, Direction.PLAIN -> 0
                    Direction.NEXT -> 1
                    else -> return null
                }

                AbsoluteUnit.YEAR -> year = when (direction) {
                    Direction.LAST -> -1
                    Direction.THIS, Direction.PLAIN -> 0
                    Direction.NEXT -> 1
                    else -> return null
                }
            }
        }

        return relativeDateTimeFormatter.localizedStringFromDateComponents(components)
    }

    override fun formatRelative(quantity: Double, direction: Direction, unit: RelativeUnit): String {
        val signed = when (direction) {
            Direction.LAST, Direction.LAST_2 -> -quantity
            Direction.NEXT, Direction.NEXT_2 -> quantity
            Direction.THIS, Direction.PLAIN -> 0.0
        }.toLong()

        val components = NSDateComponents().apply {
            when (unit) {
                RelativeUnit.SECONDS -> second = signed
                RelativeUnit.MINUTES -> minute = signed
                RelativeUnit.HOURS -> hour = signed
                RelativeUnit.DAYS -> day = signed
                RelativeUnit.MONTHS -> month = signed
                RelativeUnit.YEARS -> year = signed
            }
        }

        return relativeDateTimeFormatter.localizedStringFromDateComponents(components)
    }

    private fun LocalDateTime.toNSDate(): NSDate {
        val components = toNSDateComponents()
        return calendar.dateFromComponents(components)!!
    }

    private fun LocalDate.toNSDate(): NSDate {
        val components = toNSDateComponents()
        return calendar.dateFromComponents(components)!!
    }

    private fun LocalTime.toNSDate(): NSDate {
        val components = NSDateComponents()
        components.setHour(hour.toLong())
        components.setMinute(minute.toLong())
        components.setSecond(second.toLong())
        components.setNanosecond(nanosecond.toLong())
        return calendar.dateFromComponents(components)!!
    }

    private fun FormatStyle.toNSDateFormatterStyle(): ULong = when (this) {
        FormatStyle.SHORT -> NSDateFormatterShortStyle
        FormatStyle.MEDIUM -> NSDateFormatterMediumStyle
        FormatStyle.LONG -> NSDateFormatterLongStyle
        FormatStyle.FULL -> NSDateFormatterFullStyle
    }

    private fun CapitalizationMode.toNSFormattingContext(): NSFormattingContext = when (this) {
        CapitalizationMode.NONE -> NSFormattingContextUnknown
        CapitalizationMode.MIDDLE_OF_SENTENCE -> NSFormattingContextMiddleOfSentence
        CapitalizationMode.BEGINNING_OF_SENTENCE -> NSFormattingContextBeginningOfSentence
        CapitalizationMode.UI_LIST_OR_MENU -> NSFormattingContextListItem
        CapitalizationMode.STANDALONE -> NSFormattingContextStandalone
    }
}
