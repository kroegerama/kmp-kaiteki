package com.kroegerama.kmp.kaiteki.locale

import com.kroegerama.kmp.kaiteki.RobolectricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CallingCodesTest : RobolectricTest() {

    @Test
    fun singleCountryCodes() {
        assertEquals(49, isoCountryCodeToCallingCode("DE"))
        assertEquals(33, isoCountryCodeToCallingCode("FR"))
        assertEquals(81, isoCountryCodeToCallingCode("JP"))
        assertEquals(383, isoCountryCodeToCallingCode("XK"))
    }

    @Test
    fun sharedCodesMapPerCountry() {
        listOf("US", "CA", "JM").forEach { assertEquals(1, isoCountryCodeToCallingCode(it), it) }
        listOf("RU", "KZ").forEach { assertEquals(7, isoCountryCodeToCallingCode(it), it) }
        listOf("GB", "GG", "JE", "IM").forEach { assertEquals(44, isoCountryCodeToCallingCode(it), it) }
    }

    @Test
    fun lookupIsCaseInsensitive() {
        assertEquals(49, isoCountryCodeToCallingCode("de"))
        assertEquals(49, isoCountryCodeToCallingCode("De"))
    }

    @Test
    fun invalidCodesReturnNull() {
        listOf("", "D", "DEU", "1A", "001", "AQ").forEach { assertNull(isoCountryCodeToCallingCode(it), it) }
    }

    @Test
    fun nonAsciiCodesReturnNull() {
        listOf("ß", "ıd", "ſe").forEach { assertNull(isoCountryCodeToCallingCode(it), it) }
    }

    @Test
    fun countryListIsSortedAndUnique() {
        val isoCodes = callingCodeCountries.map { it.isoCode }
        assertEquals(isoCodes.size, isoCodes.toSet().size)
        assertEquals(isoCodes.sorted(), isoCodes)
        isoCodes.forEach { isoCode ->
            assertTrue(isoCode.length == 2 && isoCode.all { it in 'A'..'Z' }, isoCode)
        }
    }

    @Test
    fun countryListMatchesLookup() {
        callingCodeCountries.forEach { assertEquals(it.callingCode, isoCountryCodeToCallingCode(it.isoCode), it.isoCode) }
    }

    @Test
    fun localeCallingCode() {
        assertEquals(49, createPlatformLocale("de-DE").callingCode)
        assertNull(createPlatformLocale("de").callingCode)
    }

    @Test
    fun everyCountryHasDisplayName() {
        val displayLocale = createPlatformLocale("en")
        val missing = callingCodeCountries.map { it.isoCode }.filter { isoCode ->
            val name = createPlatformLocale("und-$isoCode").getDisplayCountry(displayLocale)
            name.isBlank() || name == isoCode
        }
        assertTrue(missing.isEmpty(), "No display name for: $missing")
    }
}
