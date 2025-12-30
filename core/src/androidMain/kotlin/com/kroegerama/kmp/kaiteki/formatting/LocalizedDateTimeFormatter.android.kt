package com.kroegerama.kmp.kaiteki.formatting

import android.icu.text.DisplayContext
import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.ULocale
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
import java.time.format.DecimalStyle
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import java.time.format.FormatStyle as JvmFormatStyle

@RememberInComposition
public actual fun defaultLocalizedDateTimeFormatter(
    locale: Locale,
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
    override val locale: Locale,
    dateStyle: FormatStyle,
    timeStyle: FormatStyle,
    capitalizationMode: CapitalizationMode,
    zone: TimeZone
) : LocalizedDateTimeFormatter {

    private val javaLocale = locale.toJavaLocale()
    private val javaZone = zone.toJavaZoneId()
    private val decimalStyle = DecimalStyle.of(javaLocale)

    private val dateFormatter = DateTimeFormatter
        .ofLocalizedDate(dateStyle.jvmFormatStyle())
        .withLocale(javaLocale)
        .withDecimalStyle(decimalStyle)
        .withZone(javaZone)

    private val timeFormatter = DateTimeFormatter
        .ofLocalizedTime(timeStyle.jvmFormatStyle())
        .withLocale(javaLocale)
        .withDecimalStyle(decimalStyle)
        .withZone(javaZone)

    private val dateTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(dateStyle.jvmFormatStyle(), timeStyle.jvmFormatStyle())
        .withLocale(javaLocale)
        .withDecimalStyle(decimalStyle)
        .withZone(javaZone)

    private val relativeDateTimeFormatter = RelativeDateTimeFormatter
        .getInstance(
            ULocale.forLocale(javaLocale),
            null,
            RelativeDateTimeFormatter.Style.LONG,
            capitalizationMode.toCapitalizationContext()
        )

    override fun formatDate(instant: Instant): String = dateFormatter.format(instant.toJavaInstant())
    override fun formatDate(localDate: LocalDate): String = dateFormatter.format(localDate.toJavaLocalDate())
    override fun formatDate(localDateTime: LocalDateTime): String = dateFormatter.format(localDateTime.toJavaLocalDateTime())

    override fun formatTime(instant: Instant): String = timeFormatter.format(instant.toJavaInstant())
    override fun formatTime(localTime: LocalTime): String = timeFormatter.format(localTime.toJavaLocalTime())
    override fun formatTime(localDateTime: LocalDateTime): String = timeFormatter.format(localDateTime.toJavaLocalDateTime())

    override fun formatDateTime(instant: Instant): String = dateTimeFormatter.format(instant.toJavaInstant())
    override fun formatDateTime(localDateTime: LocalDateTime): String = dateTimeFormatter.format(localDateTime.toJavaLocalDateTime())

    override fun formatRelative(direction: Direction, unit: AbsoluteUnit): String? =
        relativeDateTimeFormatter.format(direction.jvmDirection(), unit.jvmAbsoluteUnit())

    override fun formatRelative(quantity: Double, direction: Direction, unit: RelativeUnit): String? =
        relativeDateTimeFormatter.format(quantity, direction.jvmDirection(), unit.jvmAbsoluteUnit())

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
        AbsoluteUnit.DAY -> RelativeDateTimeFormatter.AbsoluteUnit.DAY
        AbsoluteUnit.MONTH -> RelativeDateTimeFormatter.AbsoluteUnit.MONTH
        AbsoluteUnit.YEAR -> RelativeDateTimeFormatter.AbsoluteUnit.YEAR
        AbsoluteUnit.NOW -> RelativeDateTimeFormatter.AbsoluteUnit.NOW
    }

    private fun RelativeUnit.jvmAbsoluteUnit(): RelativeDateTimeFormatter.RelativeUnit = when (this) {
        RelativeUnit.SECONDS -> RelativeDateTimeFormatter.RelativeUnit.SECONDS
        RelativeUnit.MINUTES -> RelativeDateTimeFormatter.RelativeUnit.MINUTES
        RelativeUnit.HOURS -> RelativeDateTimeFormatter.RelativeUnit.HOURS
        RelativeUnit.DAYS -> RelativeDateTimeFormatter.RelativeUnit.DAYS
        RelativeUnit.MONTHS -> RelativeDateTimeFormatter.RelativeUnit.MONTHS
        RelativeUnit.YEARS -> RelativeDateTimeFormatter.RelativeUnit.YEARS
    }

    private fun CapitalizationMode.toCapitalizationContext() = when (this) {
        CapitalizationMode.NONE -> DisplayContext.CAPITALIZATION_NONE
        CapitalizationMode.MIDDLE_OF_SENTENCE -> DisplayContext.CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE
        CapitalizationMode.BEGINNING_OF_SENTENCE -> DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE
        CapitalizationMode.UI_LIST_OR_MENU -> DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU
        CapitalizationMode.STANDALONE -> DisplayContext.CAPITALIZATION_FOR_STANDALONE
    }
}
