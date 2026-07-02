package com.kroegerama.kmp.kaiteki.formatting

import com.kroegerama.kmp.kaiteki.RobolectricTest
import com.kroegerama.kmp.kaiteki.locale.createPlatformLocale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.expect

class DisplayNameTest : RobolectricTest() {

    private val en = createPlatformLocale("en-US")
    private val de = createPlatformLocale("de-DE")
    private val ru = createPlatformLocale("ru-RU")
    private val ja = createPlatformLocale("ja-JP")
    private val ar = createPlatformLocale("ar-EG")

    @Test
    fun monthFullEnglish() {
        expect("January") { Month.JANUARY.displayName(TextStyle.FULL, en) }
        expect("February") { Month.FEBRUARY.displayName(TextStyle.FULL, en) }
        expect("March") { Month.MARCH.displayName(TextStyle.FULL, en) }
        expect("April") { Month.APRIL.displayName(TextStyle.FULL, en) }
        expect("May") { Month.MAY.displayName(TextStyle.FULL, en) }
        expect("June") { Month.JUNE.displayName(TextStyle.FULL, en) }
        expect("July") { Month.JULY.displayName(TextStyle.FULL, en) }
        expect("August") { Month.AUGUST.displayName(TextStyle.FULL, en) }
        expect("September") { Month.SEPTEMBER.displayName(TextStyle.FULL, en) }
        expect("October") { Month.OCTOBER.displayName(TextStyle.FULL, en) }
        expect("November") { Month.NOVEMBER.displayName(TextStyle.FULL, en) }
        expect("December") { Month.DECEMBER.displayName(TextStyle.FULL, en) }
    }

    @Test
    fun monthStylesEnglish() {
        expect("January") { Month.JANUARY.displayName(TextStyle.FULL, en) }
        expect("January") { Month.JANUARY.displayName(TextStyle.FULL_STANDALONE, en) }
        expect("Jan") { Month.JANUARY.displayName(TextStyle.SHORT, en) }
        expect("Jan") { Month.JANUARY.displayName(TextStyle.SHORT_STANDALONE, en) }
        expect("J") { Month.JANUARY.displayName(TextStyle.NARROW, en) }
        expect("J") { Month.JANUARY.displayName(TextStyle.NARROW_STANDALONE, en) }
    }

    @Test
    fun monthGerman() {
        expect("Januar") { Month.JANUARY.displayName(TextStyle.FULL, de) }
        expect("März") { Month.MARCH.displayName(TextStyle.FULL, de) }
        expect("Dezember") { Month.DECEMBER.displayName(TextStyle.FULL, de) }
    }

    @Test
    fun monthStandaloneDiffersInRussian() {
        // Russian inflects month names in format context; standalone must use the nominative form.
        expect("января") { Month.JANUARY.displayName(TextStyle.FULL, ru) }
        expect("январь") { Month.JANUARY.displayName(TextStyle.FULL_STANDALONE, ru) }
    }

    @Test
    fun weekdayFullEnglish() {
        // Covers every ordinal — guards the Sunday-first symbol mapping on Apple platforms.
        expect("Monday") { DayOfWeek.MONDAY.displayName(TextStyle.FULL, en) }
        expect("Tuesday") { DayOfWeek.TUESDAY.displayName(TextStyle.FULL, en) }
        expect("Wednesday") { DayOfWeek.WEDNESDAY.displayName(TextStyle.FULL, en) }
        expect("Thursday") { DayOfWeek.THURSDAY.displayName(TextStyle.FULL, en) }
        expect("Friday") { DayOfWeek.FRIDAY.displayName(TextStyle.FULL, en) }
        expect("Saturday") { DayOfWeek.SATURDAY.displayName(TextStyle.FULL, en) }
        expect("Sunday") { DayOfWeek.SUNDAY.displayName(TextStyle.FULL, en) }
    }

    @Test
    fun weekdayStylesEnglish() {
        expect("Mon") { DayOfWeek.MONDAY.displayName(TextStyle.SHORT, en) }
        expect("Sun") { DayOfWeek.SUNDAY.displayName(TextStyle.SHORT, en) }
        expect("M") { DayOfWeek.MONDAY.displayName(TextStyle.NARROW, en) }
        expect("S") { DayOfWeek.SUNDAY.displayName(TextStyle.NARROW, en) }
    }

    @Test
    fun weekdayGerman() {
        expect("Montag") { DayOfWeek.MONDAY.displayName(TextStyle.FULL, de) }
        expect("Sonntag") { DayOfWeek.SUNDAY.displayName(TextStyle.FULL, de) }
    }

    @Test
    fun yearMonthEnglish() {
        expect("January 2026") { YearMonth(2026, Month.JANUARY).displayName(TextStyle.FULL, en) }
        expect("Jan 2026") { YearMonth(2026, Month.JANUARY).displayName(TextStyle.SHORT, en) }
        expect("December 2025") { YearMonth(2025, Month.DECEMBER).displayName(TextStyle.FULL, en) }
    }

    @Test
    fun yearMonthGerman() {
        expect("Januar 2026") { YearMonth(2026, Month.JANUARY).displayName(TextStyle.FULL, de) }
        expect("März 2026") { YearMonth(2026, Month.MARCH).displayName(TextStyle.FULL, de) }
    }

    @Test
    fun yearMonthJapanese() {
        expect("2026年1月") { YearMonth(2026, Month.JANUARY).displayName(TextStyle.FULL, ja) }
        expect("2025年12月") { YearMonth(2025, Month.DECEMBER).displayName(TextStyle.FULL, ja) }
    }

    @Test
    fun yearMonthArabicUsesLocalizedDigitsAndMonthName() {
        val text = YearMonth(2026, Month.JANUARY).displayName(TextStyle.FULL, ar)
        assertContains(text, "٢٠٢٦")
        assertContains(text, "يناير")
    }

    @Test
    fun yearMonthDistantYears() {
        expect("January 1900") { YearMonth(1900, Month.JANUARY).displayName(TextStyle.FULL, en) }
        expect("December 9999") { YearMonth(9999, Month.DECEMBER).displayName(TextStyle.FULL, en) }
    }

    @Test
    fun extremeYearsDoNotCrash() {
        // Exact output differs per platform (era handling, year padding) — only require sane output.
        assertTrue(YearMonth(1, Month.JANUARY).displayName(TextStyle.FULL, en).isNotBlank())
        assertTrue(YearMonth(-1, Month.DECEMBER).displayName(TextStyle.FULL, en).isNotBlank())
    }

    @Test
    fun unknownLocaleFallsBackGracefully() {
        val unknown = createPlatformLocale("xx-XX")
        assertTrue(Month.JANUARY.displayName(TextStyle.FULL, unknown).isNotBlank())
        assertTrue(DayOfWeek.MONDAY.displayName(TextStyle.FULL, unknown).isNotBlank())
        assertTrue(YearMonth(2026, Month.JANUARY).displayName(TextStyle.FULL, unknown).isNotBlank())
    }

    @Test
    fun serbianScriptVariantsAreDistinguished() {
        val cyrillic = createPlatformLocale("sr-Cyrl-RS")
        val latin = createPlatformLocale("sr-Latn-RS")
        expect("јануар") { Month.JANUARY.displayName(TextStyle.FULL, cyrillic) }
        expect("januar") { Month.JANUARY.displayName(TextStyle.FULL, latin) }
        expect("понедељак") { DayOfWeek.MONDAY.displayName(TextStyle.FULL, cyrillic) }
        expect("ponedeljak") { DayOfWeek.MONDAY.displayName(TextStyle.FULL, latin) }
        // Also guards the formatter caches against keys that drop the script subtag.
        assertNotEquals(
            YearMonth(2026, Month.JANUARY).displayName(TextStyle.FULL, cyrillic),
            YearMonth(2026, Month.JANUARY).displayName(TextStyle.FULL, latin),
        )
    }

    @Test
    fun yearMonthKeepsMonthAndYearForAllStylesAndMonths() {
        // Regression guard for timezone-dependent month/year rollover: every month of the year
        // must render with its own name and year, regardless of the host timezone.
        for (style in TextStyle.entries) {
            for (month in Month.entries) {
                val text = YearMonth(2026, month).displayName(style, en)
                assertContains(text, "2026", message = "year missing for $month/$style: $text")
                assertContains(text, month.displayName(style, en), message = "month missing for $month/$style: $text")
            }
        }
    }

    @Test
    fun allCombinationsProduceOutput() {
        val locales = listOf(en, de, ru, ja, ar)
        for (locale in locales) {
            for (style in TextStyle.entries) {
                for (month in Month.entries) {
                    assertTrue(month.displayName(style, locale).isNotBlank(), "blank month name for $month/$style")
                }
                for (day in DayOfWeek.entries) {
                    assertTrue(day.displayName(style, locale).isNotBlank(), "blank weekday name for $day/$style")
                }
            }
        }
    }

    @Test
    fun cachedFormattersAreLocaleAndStyleSpecific() {
        val yearMonth = YearMonth(2026, Month.MARCH)
        // Same call twice — the cached formatter must return a stable result.
        expect(yearMonth.displayName(TextStyle.FULL, en)) { yearMonth.displayName(TextStyle.FULL, en) }
        // Same style, different locale — must not collide in the cache.
        assertNotEquals(
            yearMonth.displayName(TextStyle.FULL, en),
            yearMonth.displayName(TextStyle.FULL, de),
        )
        // Same locale, different style — must not collide in the cache.
        assertNotEquals(
            yearMonth.displayName(TextStyle.FULL, en),
            yearMonth.displayName(TextStyle.SHORT, en),
        )
    }

    @Test
    fun defaultParametersProduceOutput() {
        assertTrue(Month.JANUARY.displayName().isNotBlank())
        assertTrue(DayOfWeek.MONDAY.displayName().isNotBlank())
        assertTrue(YearMonth(2026, Month.JANUARY).displayName().isNotBlank())
    }
}
