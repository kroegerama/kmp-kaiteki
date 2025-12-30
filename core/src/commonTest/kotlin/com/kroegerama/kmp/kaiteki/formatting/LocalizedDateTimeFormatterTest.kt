package com.kroegerama.kmp.kaiteki.formatting

import com.kroegerama.kmp.kaiteki.RobolectricTest
import com.vanniktech.locale.Locale
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.expect
import kotlin.time.Instant

class LocalizedDateTimeFormatterTest : RobolectricTest() {

    private val formatterDE = defaultLocalizedDateTimeFormatter(
        locale = Locale.from("de-DE"),
        dateStyle = FormatStyle.MEDIUM,
        timeStyle = FormatStyle.SHORT,
        zone = FixedOffsetTimeZone(UtcOffset(2))
    )

    private val formatterUS = defaultLocalizedDateTimeFormatter(
        locale = Locale.from("en-US"),
        dateStyle = FormatStyle.MEDIUM,
        timeStyle = FormatStyle.SHORT,
        zone = FixedOffsetTimeZone(UtcOffset(2))
    )

    private val formatterAR = defaultLocalizedDateTimeFormatter(
        locale = Locale.from("ar-EG"),
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

        expect("8:15 PM") { formatterUS.formatTime(instant).replace(" ", " ") }
        expect("8:15 PM") { formatterUS.formatTime(localDateTime).replace(" ", " ") }
        expect("8:15 PM") { formatterUS.formatTime(localTime).replace(" ", " ") }

        expect("Oct 1, 2025, 8:15 PM") {
            formatterUS.formatDateTime(instant)
                .replace(" ", " ")
                .replace(" at ", ", ")
        }
        expect("Oct 1, 2025, 8:15 PM") {
            formatterUS.formatDateTime(localDateTime)
                .replace(" ", " ")
                .replace(" at ", ", ")
        }
    }

    @Test
    fun testAR() {
        expect("٠١‏/١٠‏/٢٠٢٥") { formatterAR.formatDate(instant) }
        expect("٠١‏/١٠‏/٢٠٢٥") { formatterAR.formatDate(localDateTime) }
        expect("٠١‏/١٠‏/٢٠٢٥") { formatterAR.formatDate(localDate) }

        expect("٨:١٥ م") { formatterAR.formatTime(instant).replace(" ", " ") }
        expect("٨:١٥ م") { formatterAR.formatTime(localDateTime).replace(" ", " ") }
        expect("٨:١٥ م") { formatterAR.formatTime(localTime).replace(" ", " ") }

        expect("٠١‏/١٠‏/٢٠٢٥، ٨:١٥ م") { formatterAR.formatDateTime(instant).replace(" ", " ") }
        expect("٠١‏/١٠‏/٢٠٢٥، ٨:١٥ م") { formatterAR.formatDateTime(localDateTime).replace(" ", " ") }
    }

    private fun <T> expectAny(vararg expected: T, computation: () -> T) {
        assertContains(expected.toList(), computation())
    }

    @Test
    fun testRelativeAbsoluteUnit() {
        // YEAR
        expect(null) { formatterUS.formatRelative(Direction.LAST_2, AbsoluteUnit.YEAR) }
        expect("last year") { formatterUS.formatRelative(Direction.LAST, AbsoluteUnit.YEAR) }
        expect("this year") { formatterUS.formatRelative(Direction.THIS, AbsoluteUnit.YEAR) }
        expect("next year") { formatterUS.formatRelative(Direction.NEXT, AbsoluteUnit.YEAR) }
        expect(null) { formatterUS.formatRelative(Direction.NEXT_2, AbsoluteUnit.YEAR) }

        // MONTH
        expect(null) { formatterUS.formatRelative(Direction.LAST_2, AbsoluteUnit.MONTH) }
        expect("last month") { formatterUS.formatRelative(Direction.LAST, AbsoluteUnit.MONTH) }
        expect("this month") { formatterUS.formatRelative(Direction.THIS, AbsoluteUnit.MONTH) }
        expect("next month") { formatterUS.formatRelative(Direction.NEXT, AbsoluteUnit.MONTH) }
        expect(null) { formatterUS.formatRelative(Direction.NEXT_2, AbsoluteUnit.MONTH) }

        // DAY
        expect(null) { formatterUS.formatRelative(Direction.LAST_2, AbsoluteUnit.DAY) }
        expect("yesterday") { formatterUS.formatRelative(Direction.LAST, AbsoluteUnit.DAY) }
        expect("today") { formatterUS.formatRelative(Direction.THIS, AbsoluteUnit.DAY) }
        expect("tomorrow") { formatterUS.formatRelative(Direction.NEXT, AbsoluteUnit.DAY) }
        expect(null) { formatterUS.formatRelative(Direction.NEXT_2, AbsoluteUnit.DAY) }

        // NOW
        expect("now") { formatterUS.formatRelative(Direction.PLAIN, AbsoluteUnit.NOW) }
    }

    @Test
    fun testRelativeQuantityUnit() {
        expect("5 years ago") { formatterUS.formatRelative(5.0, Direction.LAST, RelativeUnit.YEARS) }
        expect("in 5 years") { formatterUS.formatRelative(5.0, Direction.NEXT, RelativeUnit.YEARS) }

        expect("3 months ago") { formatterUS.formatRelative(3.0, Direction.LAST, RelativeUnit.MONTHS) }
        expect("in 3 months") { formatterUS.formatRelative(3.0, Direction.NEXT, RelativeUnit.MONTHS) }

        expect("7 days ago") { formatterUS.formatRelative(7.0, Direction.LAST, RelativeUnit.DAYS) }
        expect("in 7 days") { formatterUS.formatRelative(7.0, Direction.NEXT, RelativeUnit.DAYS) }

        expect("4 hours ago") { formatterUS.formatRelative(4.0, Direction.LAST, RelativeUnit.HOURS) }
        expect("in 4 hours") { formatterUS.formatRelative(4.0, Direction.NEXT, RelativeUnit.HOURS) }

        expect("30 minutes ago") { formatterUS.formatRelative(30.0, Direction.LAST, RelativeUnit.MINUTES) }
        expect("in 30 minutes") { formatterUS.formatRelative(30.0, Direction.NEXT, RelativeUnit.MINUTES) }

        expect("45 seconds ago") { formatterUS.formatRelative(45.0, Direction.LAST, RelativeUnit.SECONDS) }
        expect("in 45 seconds") { formatterUS.formatRelative(45.0, Direction.NEXT, RelativeUnit.SECONDS) }
    }

    @Test
    fun testRelativeAbsoluteUnitDE() {
        // YEAR
        expect(null) { formatterDE.formatRelative(Direction.LAST_2, AbsoluteUnit.YEAR) }
        expect("letztes Jahr") { formatterDE.formatRelative(Direction.LAST, AbsoluteUnit.YEAR) }
        expect("dieses Jahr") { formatterDE.formatRelative(Direction.THIS, AbsoluteUnit.YEAR) }
        expect("nächstes Jahr") { formatterDE.formatRelative(Direction.NEXT, AbsoluteUnit.YEAR) }
        expect(null) { formatterDE.formatRelative(Direction.NEXT_2, AbsoluteUnit.YEAR) }

        // MONTH
        expect(null) { formatterDE.formatRelative(Direction.LAST_2, AbsoluteUnit.MONTH) }
        expect("letzten Monat") { formatterDE.formatRelative(Direction.LAST, AbsoluteUnit.MONTH) }
        expect("diesen Monat") { formatterDE.formatRelative(Direction.THIS, AbsoluteUnit.MONTH) }
        expect("nächsten Monat") { formatterDE.formatRelative(Direction.NEXT, AbsoluteUnit.MONTH) }
        expect(null) { formatterDE.formatRelative(Direction.NEXT_2, AbsoluteUnit.MONTH) }

        // DAY
        expectAny("vorgestern", null) { formatterDE.formatRelative(Direction.LAST_2, AbsoluteUnit.DAY) }
        expect("gestern") { formatterDE.formatRelative(Direction.LAST, AbsoluteUnit.DAY) }
        expect("heute") { formatterDE.formatRelative(Direction.THIS, AbsoluteUnit.DAY) }
        expect("morgen") { formatterDE.formatRelative(Direction.NEXT, AbsoluteUnit.DAY) }
        expectAny("übermorgen", null) { formatterDE.formatRelative(Direction.NEXT_2, AbsoluteUnit.DAY) }

        // NOW
        expect("jetzt") { formatterDE.formatRelative(Direction.PLAIN, AbsoluteUnit.NOW) }
    }

    @Test
    fun testRelativeQuantityUnitDE() {
        expect("vor 5 Jahren") { formatterDE.formatRelative(5.0, Direction.LAST, RelativeUnit.YEARS) }
        expect("in 5 Jahren") { formatterDE.formatRelative(5.0, Direction.NEXT, RelativeUnit.YEARS) }

        expect("vor 3 Monaten") { formatterDE.formatRelative(3.0, Direction.LAST, RelativeUnit.MONTHS) }
        expect("in 3 Monaten") { formatterDE.formatRelative(3.0, Direction.NEXT, RelativeUnit.MONTHS) }

        expect("vor 7 Tagen") { formatterDE.formatRelative(7.0, Direction.LAST, RelativeUnit.DAYS) }
        expect("in 7 Tagen") { formatterDE.formatRelative(7.0, Direction.NEXT, RelativeUnit.DAYS) }

        expect("vor 4 Stunden") { formatterDE.formatRelative(4.0, Direction.LAST, RelativeUnit.HOURS) }
        expect("in 4 Stunden") { formatterDE.formatRelative(4.0, Direction.NEXT, RelativeUnit.HOURS) }

        expect("vor 30 Minuten") { formatterDE.formatRelative(30.0, Direction.LAST, RelativeUnit.MINUTES) }
        expect("in 30 Minuten") { formatterDE.formatRelative(30.0, Direction.NEXT, RelativeUnit.MINUTES) }

        expect("vor 45 Sekunden") { formatterDE.formatRelative(45.0, Direction.LAST, RelativeUnit.SECONDS) }
        expect("in 45 Sekunden") { formatterDE.formatRelative(45.0, Direction.NEXT, RelativeUnit.SECONDS) }
    }
}
