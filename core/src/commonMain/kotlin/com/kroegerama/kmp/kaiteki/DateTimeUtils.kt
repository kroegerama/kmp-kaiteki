package com.kroegerama.kmp.kaiteki

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Returns the Julian Day number for this date.
 *
 * The Julian Day is the number of days since noon Universal Time on January 1, 4713 BC.
 *
 * Use as Replacement for the JVM version [Julian Fields](https://docs.oracle.com/javase/8/docs/api/java/time/temporal/JulianFields.html)
 */
public fun LocalDate.toJulianDay(): Long = toEpochDays() + 2440588L

/**
 * @see [LocalDate.toJulianDay]
 */
public fun LocalDateTime.toJulianDay(): Long = date.toJulianDay()

/**
 * @see [LocalDate.toJulianDay]
 */
public fun Instant.toJulianDay(): Long = toLocalDateTime(TimeZone.UTC).toJulianDay()

/**
 * Returns the number of days between this date and the other date.
 *
 * The result will be negative if the other date is later than this one.
 *
 * @see [LocalDate.toEpochDays]
 */
public infix fun LocalDate.dayDistanceTo(other: LocalDate): Long =
    toEpochDays() - other.toEpochDays()

/**
 * @see [LocalDate.dayDistanceTo]
 */
public infix fun LocalDateTime.dayDistanceTo(other: LocalDateTime): Long =
    date.dayDistanceTo(other.date)

/**
 * @see [LocalDate.dayDistanceTo]
 */
public infix fun Instant.dayDistanceTo(other: Instant): Long =
    toLocalDateTime(TimeZone.UTC).dayDistanceTo(other.toLocalDateTime(TimeZone.UTC))
