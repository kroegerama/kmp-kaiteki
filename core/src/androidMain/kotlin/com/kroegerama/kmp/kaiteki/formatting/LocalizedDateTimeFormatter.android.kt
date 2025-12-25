package com.kroegerama.kmp.kaiteki.formatting

import android.icu.text.RelativeDateTimeFormatter
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

    private val relativeDateTimeFormatter = RelativeDateTimeFormatter.getInstance(locale.toJavaLocale())

    actual override fun formatDate(instant: Instant): String = dateFormatter.format(instant.toJavaInstant())
    actual override fun formatDate(localDate: LocalDate): String = dateFormatter.format(localDate.toJavaLocalDate())
    actual override fun formatDate(localDateTime: LocalDateTime): String = dateFormatter.format(localDateTime.toJavaLocalDateTime())

    actual override fun formatTime(instant: Instant): String = timeFormatter.format(instant.toJavaInstant())
    actual override fun formatTime(localTime: LocalTime): String = timeFormatter.format(localTime.toJavaLocalTime())
    actual override fun formatTime(localDateTime: LocalDateTime): String = timeFormatter.format(localDateTime.toJavaLocalDateTime())

    actual override fun formatDateTime(instant: Instant): String = dateTimeFormatter.format(instant.toJavaInstant())
    actual override fun formatDateTime(localDateTime: LocalDateTime): String = dateTimeFormatter.format(localDateTime.toJavaLocalDateTime())

    actual override fun formatRelative(direction: Direction, unit: AbsoluteUnit): String? =
        relativeDateTimeFormatter.format(direction.jvmDirection(), unit.jvmAbsoluteUnit())

    actual override fun formatRelative(quantity: Double, direction: Direction, unit: RelativeUnit): String? =
        relativeDateTimeFormatter.format(quantity, direction.jvmDirection(), unit.jvmAbsoluteUnit())
}

private fun FormatStyle.jvmFormatStyle() = when (this) {
    FormatStyle.SHORT -> JvmFormatStyle.SHORT
    FormatStyle.MEDIUM -> JvmFormatStyle.MEDIUM
    FormatStyle.LONG -> JvmFormatStyle.LONG
    FormatStyle.FULL -> JvmFormatStyle.FULL
}

private fun Direction.jvmDirection(): RelativeDateTimeFormatter.Direction = when (this) {
    Direction.LAST_2 -> RelativeDateTimeFormatter.Direction.LAST_2
    Direction.LAST -> RelativeDateTimeFormatter.Direction.LAST
    Direction.THIS -> RelativeDateTimeFormatter.Direction.THIS
    Direction.NEXT -> RelativeDateTimeFormatter.Direction.NEXT
    Direction.NEXT_2 -> RelativeDateTimeFormatter.Direction.NEXT_2
    Direction.PLAIN -> RelativeDateTimeFormatter.Direction.PLAIN
}

private fun AbsoluteUnit.jvmAbsoluteUnit(): RelativeDateTimeFormatter.AbsoluteUnit = when (this) {
    AbsoluteUnit.SUNDAY -> RelativeDateTimeFormatter.AbsoluteUnit.SUNDAY
    AbsoluteUnit.MONDAY -> RelativeDateTimeFormatter.AbsoluteUnit.MONDAY
    AbsoluteUnit.TUESDAY -> RelativeDateTimeFormatter.AbsoluteUnit.TUESDAY
    AbsoluteUnit.WEDNESDAY -> RelativeDateTimeFormatter.AbsoluteUnit.WEDNESDAY
    AbsoluteUnit.THURSDAY -> RelativeDateTimeFormatter.AbsoluteUnit.THURSDAY
    AbsoluteUnit.FRIDAY -> RelativeDateTimeFormatter.AbsoluteUnit.FRIDAY
    AbsoluteUnit.SATURDAY -> RelativeDateTimeFormatter.AbsoluteUnit.SATURDAY
    AbsoluteUnit.DAY -> RelativeDateTimeFormatter.AbsoluteUnit.DAY
    AbsoluteUnit.WEEK -> RelativeDateTimeFormatter.AbsoluteUnit.WEEK
    AbsoluteUnit.MONTH -> RelativeDateTimeFormatter.AbsoluteUnit.MONTH
    AbsoluteUnit.YEAR -> RelativeDateTimeFormatter.AbsoluteUnit.YEAR
    AbsoluteUnit.NOW -> RelativeDateTimeFormatter.AbsoluteUnit.NOW
}

private fun RelativeUnit.jvmAbsoluteUnit(): RelativeDateTimeFormatter.RelativeUnit = when (this) {
    RelativeUnit.SECONDS -> RelativeDateTimeFormatter.RelativeUnit.SECONDS
    RelativeUnit.MINUTES -> RelativeDateTimeFormatter.RelativeUnit.MINUTES
    RelativeUnit.HOURS -> RelativeDateTimeFormatter.RelativeUnit.HOURS
    RelativeUnit.DAYS -> RelativeDateTimeFormatter.RelativeUnit.DAYS
    RelativeUnit.WEEKS -> RelativeDateTimeFormatter.RelativeUnit.WEEKS
    RelativeUnit.MONTHS -> RelativeDateTimeFormatter.RelativeUnit.MONTHS
    RelativeUnit.YEARS -> RelativeDateTimeFormatter.RelativeUnit.YEARS
}
