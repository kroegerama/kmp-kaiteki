package com.kroegerama.kmp.kaiteki.locale

import com.kroegerama.kmp.kaiteki.RobolectricTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleUtilsTest : RobolectricTest() {

    @Test
    fun isoCountryCodeToFlagValid() {
        assertEquals("\uD83C\uDDE9\uD83C\uDDEA", isoCountryCodeToFlag("DE")) // 🇩🇪
        assertEquals("\uD83C\uDDFA\uD83C\uDDF8", isoCountryCodeToFlag("US")) // 🇺🇸
        assertEquals("\uD83C\uDDEB\uD83C\uDDF7", isoCountryCodeToFlag("FR")) // 🇫🇷
        assertEquals("\uD83C\uDDEF\uD83C\uDDF5", isoCountryCodeToFlag("JP")) // 🇯🇵
        assertEquals("\uD83C\uDDE7\uD83C\uDDF7", isoCountryCodeToFlag("BR")) // 🇧🇷
        assertEquals("\uD83C\uDDEC\uD83C\uDDE7", isoCountryCodeToFlag("GB")) // 🇬🇧
        assertEquals("\uD83C\uDDE8\uD83C\uDDF3", isoCountryCodeToFlag("CN")) // 🇨🇳
        assertEquals("\uD83C\uDDE6\uD83C\uDDFA", isoCountryCodeToFlag("AU")) // 🇦🇺
    }

    @Test
    fun isoCountryCodeToFlagCaseInsensitive() {
        assertEquals(isoCountryCodeToFlag("DE"), isoCountryCodeToFlag("de"))
    }

    @Test
    fun isoCountryCodeToFlagInvalidLength() {
        assertEquals("", isoCountryCodeToFlag("D"))
        assertEquals("", isoCountryCodeToFlag("DEU"))
    }

    @Test
    fun isoCountryCodeToFlagInvalidChars() {
        assertEquals("", isoCountryCodeToFlag("1A"))
    }

    @Test
    fun isoCountryCodeToFlagCustomFallback() {
        assertEquals("??", isoCountryCodeToFlag("1A", fallback = "??"))
    }

    @Test
    fun unicodeFlagForLocaleWithCountry() {
        val locale = Locales.GERMANY
        assertEquals("\uD83C\uDDE9\uD83C\uDDEA", locale.unicodeFlag)
    }

    @Test
    fun unicodeFlagForLocaleWithoutCountry() {
        val locale = Locales.GERMAN
        assertEquals("", locale.unicodeFlag)
    }
}
