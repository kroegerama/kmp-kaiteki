package com.kroegerama.kmp.kaiteki.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.expect
import kotlin.time.Clock
import kotlin.time.Instant

private fun fixedClock(instant: Instant) = object : Clock {
    override fun now() = instant
}

class DateTimeUtilsTest {

    private val fixedInstant = Instant.parse("2025-06-15T10:30:45Z")
    private val clock = fixedClock(fixedInstant)

    @Test
    fun localDateTimeNowUTC() {
        expect(LocalDateTime(2025, 6, 15, 10, 30, 45)) {
            LocalDateTime.now(TimeZone.UTC, clock)
        }
    }

    @Test
    fun localDateTimeNowWithOffset() {
        expect(LocalDateTime(2025, 6, 15, 12, 30, 45)) {
            LocalDateTime.now(TimeZone.of("UTC+2"), clock)
        }
    }

    @Test
    fun localDateNow() {
        expect(LocalDate(2025, 6, 15)) {
            LocalDate.now(TimeZone.UTC, clock)
        }
    }

    @Test
    fun localDateNowWithOffset() {
        // 2025-06-15T23:30:45Z in UTC+2 is 2025-06-16T01:30:45
        val lateNightClock = fixedClock(Instant.parse("2025-06-15T23:30:45Z"))
        expect(LocalDate(2025, 6, 16)) {
            LocalDate.now(TimeZone.of("UTC+2"), lateNightClock)
        }
    }

    @Test
    fun localTimeNow() {
        expect(LocalTime(10, 30, 45)) {
            LocalTime.now(TimeZone.UTC, clock)
        }
    }

    @Test
    fun localTimeNowWithOffset() {
        expect(LocalTime(12, 30, 45)) {
            LocalTime.now(TimeZone.of("UTC+2"), clock)
        }
    }

    @Test
    fun yearMonthNow() {
        expect(YearMonth(2025, 6)) {
            YearMonth.now(TimeZone.UTC, clock)
        }
    }

    @Test
    fun yearMonthNowWithOffset() {
        // 2025-06-30T23:30:45Z in UTC+2 is 2025-07-01T01:30:45
        val lateNightClock = fixedClock(Instant.parse("2025-06-30T23:30:45Z"))
        expect(YearMonth(2025, 7)) {
            YearMonth.now(TimeZone.of("UTC+2"), lateNightClock)
        }
    }

    @Test
    fun toMinuteOfDay() {
        expect(0) { LocalTime(0, 0).toMinuteOfDay() }
        expect(60) { LocalTime(1, 0).toMinuteOfDay() }
        expect(630) { LocalTime(10, 30).toMinuteOfDay() }
        expect(1439) { LocalTime(23, 59).toMinuteOfDay() }
    }

    @Test
    fun fromMinuteOfDay() {
        expect(LocalTime(0, 0)) { LocalTime.fromMinuteOfDay(0) }
        expect(LocalTime(1, 0)) { LocalTime.fromMinuteOfDay(60) }
        expect(LocalTime(10, 30)) { LocalTime.fromMinuteOfDay(630) }
        expect(LocalTime(23, 59)) { LocalTime.fromMinuteOfDay(1439) }
    }

    @Test
    fun fromMinuteOfDayInvalid() {
        assertFails { LocalTime.fromMinuteOfDay(-1) }
        assertFails { LocalTime.fromMinuteOfDay(1440) }
    }
}
