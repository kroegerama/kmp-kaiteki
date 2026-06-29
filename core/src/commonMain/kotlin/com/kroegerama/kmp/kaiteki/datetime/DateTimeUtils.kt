package com.kroegerama.kmp.kaiteki.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import kotlin.time.Clock

public fun LocalDateTime.Companion.now(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    clock: Clock = Clock.System
): LocalDateTime = clock.now().toLocalDateTime(timeZone)

public fun LocalDate.Companion.now(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    clock: Clock = Clock.System
): LocalDate = clock.todayIn(timeZone)

public fun LocalTime.Companion.now(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    clock: Clock = Clock.System
): LocalTime = LocalDateTime.now(timeZone, clock).time

public fun YearMonth.Companion.now(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    clock: Clock = Clock.System
): YearMonth = LocalDate.now(timeZone, clock).yearMonth

internal const val MINUTES_PER_HOUR = 60
internal const val MINUTES_PER_DAY = MINUTES_PER_HOUR * 24

public fun LocalTime.toMinuteOfDay(): Int {
    var total: Int = hour * MINUTES_PER_HOUR
    total += minute
    return total
}

public fun LocalTime.Companion.fromMinuteOfDay(minuteOfDay: Int): LocalTime {
    require(minuteOfDay in 0 until MINUTES_PER_DAY) {
        "Invalid time: minuteOfDay must be between 0 and $MINUTES_PER_DAY, got $minuteOfDay"
    }
    return LocalTime(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)
}
