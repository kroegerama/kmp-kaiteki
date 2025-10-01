package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.annotation.RememberInComposition
import com.vanniktech.locale.Locale
import com.vanniktech.locale.toJavaLocale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toJavaZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import java.time.format.FormatStyle as JvmFormatStyle

public actual class DefaultLocalizedDateTimeFormatter
@RememberInComposition actual constructor(
    locale: Locale,
    dateStyle: FormatStyle,
    timeStyle: FormatStyle,
    zone: TimeZone
) : LocalizedDateTimeFormatter {

    private val dateFormatter = DateTimeFormatter
        .ofLocalizedDate(dateStyle.jvmFormatStyle())
        .withLocale(locale.toJavaLocale())
        .withZone(zone.toJavaZoneId())

    private val timeFormatter = DateTimeFormatter
        .ofLocalizedTime(timeStyle.jvmFormatStyle())
        .withLocale(locale.toJavaLocale())
        .withZone(zone.toJavaZoneId())

    private val dateTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(dateStyle.jvmFormatStyle(), timeStyle.jvmFormatStyle())
        .withLocale(locale.toJavaLocale())
        .withZone(zone.toJavaZoneId())

    actual override fun formatDate(instant: Instant): String = dateFormatter.format(instant.toJavaInstant())
    actual override fun formatDate(localDate: LocalDate): String = dateFormatter.format(localDate.toJavaLocalDate())
    actual override fun formatDate(localDateTime: LocalDateTime): String = dateFormatter.format(localDateTime.toJavaLocalDateTime())

    actual override fun formatTime(instant: Instant): String = timeFormatter.format(instant.toJavaInstant())
    actual override fun formatTime(localTime: LocalTime): String = timeFormatter.format(localTime.toJavaLocalTime())
    actual override fun formatTime(localDateTime: LocalDateTime): String = timeFormatter.format(localDateTime.toJavaLocalDateTime())

    actual override fun formatDateTime(instant: Instant): String = dateTimeFormatter.format(instant.toJavaInstant())
    actual override fun formatDateTime(localDateTime: LocalDateTime): String = dateTimeFormatter.format(localDateTime.toJavaLocalDateTime())
}

private fun FormatStyle.jvmFormatStyle() = when (this) {
    FormatStyle.SHORT -> JvmFormatStyle.SHORT
    FormatStyle.MEDIUM -> JvmFormatStyle.MEDIUM
    FormatStyle.LONG -> JvmFormatStyle.LONG
    FormatStyle.FULL -> JvmFormatStyle.FULL
}
