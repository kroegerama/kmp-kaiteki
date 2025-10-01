package com.kroegerama.kmp.kaiteki.formatting

import com.vanniktech.locale.Locale
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlin.test.Test
import kotlin.test.expect
import kotlin.time.Instant

class LocalizedDateTimeFormatterTest {

    private val formatterDE = DefaultLocalizedDateTimeFormatter(
        locale = Locale.from("de-DE"),
        dateStyle = FormatStyle.MEDIUM,
        timeStyle = FormatStyle.SHORT,
        zone = FixedOffsetTimeZone(UtcOffset(2))
    )

    private val formatterUS = DefaultLocalizedDateTimeFormatter(
        locale = Locale.from("en-US"),
        dateStyle = FormatStyle.MEDIUM,
        timeStyle = FormatStyle.SHORT,
        zone = FixedOffsetTimeZone(UtcOffset(2))
    )

    private val instant = Instant.parse("2025-10-01T18:15:00Z")
    private val localDateTime = LocalDateTime.parse("2025-10-01T20:15:00")
    private val localDate = LocalDate.parse("2025-10-01")
    private val localTime = LocalTime.parse("20:15:00")

    @Test
    fun testDE() {
        expect("01.10.2025") { formatterDE.formatDate(instant) }
        expect("01.10.2025") { formatterDE.formatDate(localDateTime) }
        expect("01.10.2025") { formatterDE.formatDate(localDate) }

        expect("20:15") { formatterDE.formatTime(instant) }
        expect("20:15") { formatterDE.formatTime(localDateTime) }
        expect("20:15") { formatterDE.formatTime(localTime) }

        expect("01.10.2025, 20:15") { formatterDE.formatDateTime(instant) }
        expect("01.10.2025, 20:15") { formatterDE.formatDateTime(localDateTime) }
    }

    @Test
    fun testUS() {
        expect("Oct 1, 2025") { formatterUS.formatDate(instant) }
        expect("Oct 1, 2025") { formatterUS.formatDate(localDateTime) }
        expect("Oct 1, 2025") { formatterUS.formatDate(localDate) }

        expect("8:15 PM") { formatterUS.formatTime(instant) }
        expect("8:15 PM") { formatterUS.formatTime(localDateTime) }
        expect("8:15 PM") { formatterUS.formatTime(localTime) }

        expect("Oct 1, 2025, 8:15 PM") { formatterUS.formatDateTime(instant) }
        expect("Oct 1, 2025, 8:15 PM") { formatterUS.formatDateTime(localDateTime) }
    }
}
