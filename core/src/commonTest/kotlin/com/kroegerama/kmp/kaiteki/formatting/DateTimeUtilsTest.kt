package com.kroegerama.kmp.kaiteki.formatting

import com.kroegerama.kmp.kaiteki.dayDistanceTo
import com.kroegerama.kmp.kaiteki.toJulianDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.expect
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class DateTimeUtilsTest {

    @Test
    fun julianDayInstantTest() {
        expect(2_460_967) { Instant.parse("2025-10-18T10:34:56.789+02:00").toJulianDay() }
    }

    @Test
    fun julianDayDateTimeTest() {
        expect(2_460_967) { LocalDateTime.parse("2025-10-18T00:00:00").toJulianDay() }
        expect(2_460_967) { LocalDateTime.parse("2025-10-18T23:59:00").toJulianDay() }
    }

    @Test
    fun julianDayDateTest() {
        expect(2_460_967) { LocalDate.parse("2025-10-18").toJulianDay() }
    }

    /**
     * See [JulianFields](https://docs.oracle.com/javase/8/docs/api/java/time/temporal/JulianFields.html)
     */
    @Test
    fun julianDayJavaCompatTest() {
        expect(2_440_588L) { LocalDateTime.parse("1970-01-01T00:00").toJulianDay() }
        expect(2_440_588L) { LocalDateTime.parse("1970-01-01T06:00").toJulianDay() }
        expect(2_440_588L) { LocalDateTime.parse("1970-01-01T12:00").toJulianDay() }
        expect(2_440_588L) { LocalDateTime.parse("1970-01-01T18:00").toJulianDay() }
        expect(2_440_589L) { LocalDateTime.parse("1970-01-02T00:00").toJulianDay() }
        expect(2_440_589L) { LocalDateTime.parse("1970-01-02T06:00").toJulianDay() }
        expect(2_440_589L) { LocalDateTime.parse("1970-01-02T12:00").toJulianDay() }
    }

    @Test
    fun dayDistanceTest() {
        val now = Instant.parse("2025-10-18T12:34:56.789+02:00")
        val tz = TimeZone.of("Europe/Berlin")

        expect(-1) { now.minus(1.days).dayDistanceTo(now, tz) }
        expect(0) { now.minus(12.hours).dayDistanceTo(now, tz) }
        expect(0) { now.dayDistanceTo(now, tz) }
        expect(0) { now.plus(11.hours).dayDistanceTo(now, tz) }
        expect(1) { now.plus(1.days).dayDistanceTo(now, tz) }
    }
}
