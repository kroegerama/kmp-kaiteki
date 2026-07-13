package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Stable
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import com.kroegerama.kmp.kaiteki.locale.currentPlatformLocale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

/**
 * Localized name of this month, e.g. `"January"` or `"Jan"`.
 *
 * @param style length and standalone/formatting variant of the name.
 * @param locale locale to translate into.
 */
@Stable
public expect fun Month.displayName(
    style: TextStyle = TextStyle.FULL,
    locale: PlatformLocale = currentPlatformLocale()
): String

/**
 * Localized name of this day of week, e.g. `"Monday"` or `"Mon"`.
 *
 * @param style length and standalone/formatting variant of the name.
 * @param locale locale to translate into.
 */
@Stable
public expect fun DayOfWeek.displayName(
    style: TextStyle = TextStyle.FULL,
    locale: PlatformLocale = currentPlatformLocale()
): String

/**
 * Localized name of this year-month, e.g. `"January 2026"` or `"Jan 2026"`.
 *
 * @param style length and standalone/formatting variant of the name.
 * @param locale locale to translate into.
 */
@Stable
public expect fun YearMonth.displayName(
    style: TextStyle = TextStyle.FULL,
    locale: PlatformLocale = currentPlatformLocale()
): String
