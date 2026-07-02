package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Stable
import com.ibm.icu.text.DateTimePatternGenerator
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaMonth
import kotlinx.datetime.toJavaYearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.concurrent.ConcurrentHashMap
import java.time.format.TextStyle as JvmTextStyle

@Stable
public actual fun Month.displayName(
    style: TextStyle,
    locale: PlatformLocale
): String = toJavaMonth().getDisplayName(style.platformStyle, locale)

@Stable
public actual fun DayOfWeek.displayName(
    style: TextStyle,
    locale: PlatformLocale
): String = toJavaDayOfWeek().getDisplayName(style.platformStyle, locale)

@Stable
public actual fun YearMonth.displayName(
    style: TextStyle,
    locale: PlatformLocale
): String {
    val format = DisplayNameUtils.getYearMonthFormat(style, locale)
    return toJavaYearMonth().format(format)
}

private val TextStyle.platformStyle: JvmTextStyle
    get() = when (this) {
        TextStyle.FULL -> JvmTextStyle.FULL
        TextStyle.FULL_STANDALONE -> JvmTextStyle.FULL_STANDALONE
        TextStyle.SHORT -> JvmTextStyle.SHORT
        TextStyle.SHORT_STANDALONE -> JvmTextStyle.SHORT_STANDALONE
        TextStyle.NARROW -> JvmTextStyle.NARROW
        TextStyle.NARROW_STANDALONE -> JvmTextStyle.NARROW_STANDALONE
    }

private object DisplayNameUtils {
    private val yearMonthFormatCache = ConcurrentHashMap<String, DateTimeFormatter>()

    private fun key(style: TextStyle, locale: PlatformLocale) = "${style.name}|${locale.toLanguageTag()}"

    fun getYearMonthFormat(style: TextStyle, locale: PlatformLocale): DateTimeFormatter = yearMonthFormatCache.getOrPut(key(style, locale)) {
        val skeleton = style.toYearMonthSkeleton()
        val generator = DateTimePatternGenerator.getInstance(locale)
        val pattern = generator.getBestPattern(skeleton)
        // ofPattern alone keeps ASCII digits; DecimalStyle localizes them (e.g. Arabic-Indic for ar-EG).
        DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.of(locale))
    }
}
