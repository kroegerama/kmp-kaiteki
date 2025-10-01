package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.annotation.RememberInComposition
import com.vanniktech.locale.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toNSDate
import kotlinx.datetime.toNSDateComponents
import kotlinx.datetime.toNSTimeZone
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import kotlin.time.Instant

public actual class DefaultLocalizedDateTimeFormatter
@RememberInComposition actual constructor(
    locale: Locale,
    dateStyle: FormatStyle,
    timeStyle: FormatStyle,
    zone: TimeZone
) : LocalizedDateTimeFormatter {
    private val dateFormatter: NSDateFormatter = NSDateFormatter().apply {
        this.locale = NSLocale(locale.toString())
        this.timeZone = zone.toNSTimeZone()
        this.dateStyle = dateStyle.toNSDateFormatterStyle()
        this.timeStyle = NSDateFormatterNoStyle
    }
    private val timeFormatter: NSDateFormatter = NSDateFormatter().apply {
        this.locale = NSLocale(locale.toString())
        this.timeZone = zone.toNSTimeZone()
        this.dateStyle = NSDateFormatterNoStyle
        this.timeStyle = timeStyle.toNSDateFormatterStyle()
    }
    private val dateTimeFormatter: NSDateFormatter = NSDateFormatter().apply {
        this.locale = NSLocale(locale.toString())
        this.timeZone = zone.toNSTimeZone()
        this.dateStyle = dateStyle.toNSDateFormatterStyle()
        this.timeStyle = timeStyle.toNSDateFormatterStyle()
    }
    private val calendar = NSCalendar.currentCalendar.apply {
        setTimeZone(zone.toNSTimeZone())
    }

    actual override fun formatDate(instant: Instant): String = dateFormatter.stringFromDate(instant.toNSDate())
    actual override fun formatDate(localDate: LocalDate): String = dateFormatter.stringFromDate(localDate.toNSDate())
    actual override fun formatDate(localDateTime: LocalDateTime): String = dateFormatter.stringFromDate(localDateTime.toNSDate())

    actual override fun formatTime(instant: Instant): String = timeFormatter.stringFromDate(instant.toNSDate())
    actual override fun formatTime(localTime: LocalTime): String = timeFormatter.stringFromDate(localTime.toNSDate())
    actual override fun formatTime(localDateTime: LocalDateTime): String = timeFormatter.stringFromDate(localDateTime.toNSDate())

    actual override fun formatDateTime(instant: Instant): String = dateTimeFormatter.stringFromDate(instant.toNSDate())
    actual override fun formatDateTime(localDateTime: LocalDateTime): String = dateTimeFormatter.stringFromDate(localDateTime.toNSDate())

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
}

private fun FormatStyle.toNSDateFormatterStyle(): ULong = when (this) {
    FormatStyle.SHORT -> NSDateFormatterShortStyle
    FormatStyle.MEDIUM -> NSDateFormatterMediumStyle
    FormatStyle.LONG -> NSDateFormatterLongStyle
    FormatStyle.FULL -> NSDateFormatterFullStyle
}
