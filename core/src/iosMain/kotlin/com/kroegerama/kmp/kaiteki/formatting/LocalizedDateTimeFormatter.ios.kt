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
        setLocalizedDateFormatFromTemplate(dateStyle.toDateTemplate())
    }
    private val timeFormatter: NSDateFormatter = NSDateFormatter().apply {
        this.locale = NSLocale(locale.toString())
        this.timeZone = zone.toNSTimeZone()
        setLocalizedDateFormatFromTemplate(timeStyle.toTimeTemplate())
    }
    private val dateTimeFormatter: NSDateFormatter = NSDateFormatter().apply {
        this.locale = NSLocale(locale.toString())
        this.timeZone = zone.toNSTimeZone()
        setLocalizedDateFormatFromTemplate("${dateStyle.toDateTemplate()}, ${timeStyle.toTimeTemplate()}")
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
        val calendar = NSCalendar.currentCalendar
        val components = toNSDateComponents()
        return calendar.dateFromComponents(components)!!
    }

    private fun LocalDate.toNSDate(): NSDate {
        val calendar = NSCalendar.currentCalendar
        val components = toNSDateComponents()
        return calendar.dateFromComponents(components)!!
    }

    private fun LocalTime.toNSDate(): NSDate {
        val calendar = NSCalendar.currentCalendar
        val components = NSDateComponents()
        components.setHour(hour.toLong())
        components.setMinute(minute.toLong())
        components.setSecond(second.toLong())
        components.setNanosecond(nanosecond.toLong())
        return calendar.dateFromComponents(components)!!
    }
}

private fun FormatStyle.toDateTemplate() = when (this) {
    FormatStyle.SHORT -> "yMMd"
    FormatStyle.MEDIUM -> "yMMMd"
    FormatStyle.LONG -> "yMMMMd"
    FormatStyle.FULL -> "yMMMMEEEEd"
}

private fun FormatStyle.toTimeTemplate() = when (this) {
    FormatStyle.SHORT -> "jm"
    FormatStyle.MEDIUM -> "jms"
    FormatStyle.LONG -> "jmsz"
    FormatStyle.FULL -> "jmszzzz"
}
