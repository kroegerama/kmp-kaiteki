package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Stable
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import com.kroegerama.kmp.kaiteki.locale.currentPlatformLocale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

@Stable
public expect fun Month.displayName(
    style: TextStyle = TextStyle.FULL,
    locale: PlatformLocale = currentPlatformLocale()
): String

@Stable
public expect fun DayOfWeek.displayName(
    style: TextStyle = TextStyle.FULL,
    locale: PlatformLocale = currentPlatformLocale()
): String

@Stable
public expect fun YearMonth.displayName(
    style: TextStyle = TextStyle.FULL,
    locale: PlatformLocale = currentPlatformLocale()
): String
