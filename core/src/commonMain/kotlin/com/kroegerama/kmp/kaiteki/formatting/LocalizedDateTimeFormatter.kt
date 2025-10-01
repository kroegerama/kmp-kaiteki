package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import com.vanniktech.locale.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

public enum class FormatStyle { SHORT, MEDIUM, LONG, FULL }

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
}
