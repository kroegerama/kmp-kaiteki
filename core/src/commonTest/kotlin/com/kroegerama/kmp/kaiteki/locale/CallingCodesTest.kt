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
    fun phoneNumberWithSingleRegionCode() {
        assertEquals(CallingCodeCountry("DE", 49), callingCodeCountryForPhoneNumber("+49 30 123456"))
        assertEquals(CallingCodeCountry("DE", 49), callingCodeCountryForPhoneNumber("0049 30 123456"))
        assertEquals(CallingCodeCountry("DE", 49), callingCodeCountryForPhoneNumber("  +49  "))
    }

    @Test
    fun phoneNumberWithSharedCodeResolvesByPrefix() {
        assertEquals(CallingCodeCountry("BS", 1), callingCodeCountryForPhoneNumber("+1 242 555 0100"))
        assertEquals(CallingCodeCountry("CA", 1), callingCodeCountryForPhoneNumber("+1 416 555 0100"))
        assertEquals(CallingCodeCountry("US", 1), callingCodeCountryForPhoneNumber("+1 212 555 0100"))
        assertEquals(CallingCodeCountry("KZ", 7), callingCodeCountryForPhoneNumber("+7 701 123 4567"))
        assertEquals(CallingCodeCountry("RU", 7), callingCodeCountryForPhoneNumber("+7 495 123 4567"))
        assertEquals(CallingCodeCountry("AX", 358), callingCodeCountryForPhoneNumber("+358 18 123 456"))
        assertEquals(CallingCodeCountry("FI", 358), callingCodeCountryForPhoneNumber("+358 9 123 456"))
        assertEquals(CallingCodeCountry("VA", 39), callingCodeCountryForPhoneNumber("+39 06 6981 2345"))
        assertEquals(CallingCodeCountry("IT", 39), callingCodeCountryForPhoneNumber("+39 06 1234 5678"))
    }

    @Test
    fun phoneNumberWithUnknownPrefixFallsBackToMainRegion() {
        assertEquals(CallingCodeCountry("US", 1), callingCodeCountryForPhoneNumber("+1"))
        assertEquals(CallingCodeCountry("US", 1), callingCodeCountryForPhoneNumber("+1 999 555 0100"))
        assertEquals(CallingCodeCountry("US", 1), callingCodeCountryForPhoneNumber("+1 24"))
        assertEquals(CallingCodeCountry("GB", 44), callingCodeCountryForPhoneNumber("+44 20 7946 0958"))
        assertEquals(CallingCodeCountry("US", 1), callingCodeCountryForPhoneNumber("+1" + "9".repeat(10_000)))
    }

    @Test
    fun storedPrefixesFitTheLookupLength() {
        val tooLong = regionPrefixTable.values.flatMap { it.keys }.filter { it.length > MAX_REGION_PREFIX_LENGTH }
        assertEquals(emptyList(), tooLong)
    }

    @Test
    fun phoneNumberFormattingCharactersAreIgnored() {
        assertEquals(CallingCodeCountry("BS", 1), callingCodeCountryForPhoneNumber("+1 (242) 555-0100"))
        assertEquals(CallingCodeCountry("BS", 1), callingCodeCountryForPhoneNumber("001.242.555.0100"))
        assertEquals(CallingCodeCountry("BS", 1), callingCodeCountryForPhoneNumber("‪+1 242 555 0100‬"))
        assertEquals(CallingCodeCountry("BS", 1), callingCodeCountryForPhoneNumber("tel:+12425550100"))
        assertEquals(CallingCodeCountry("BS", 1), callingCodeCountryForPhoneNumber("(+1) 242 555 0100"))
    }

    @Test
    fun phoneNumberTrunkZeroInParenthesesIsIgnored() {
        assertEquals(CallingCodeCountry("IM", 44), callingCodeCountryForPhoneNumber("+44 (0)1624 123456"))
        assertEquals(CallingCodeCountry("AX", 358), callingCodeCountryForPhoneNumber("+358 (0)18 123 456"))
        assertEquals(CallingCodeCountry("VA", 39), callingCodeCountryForPhoneNumber("+39 06 6981 2345"))
    }

    @Test
    fun phoneNumberWithoutKnownCallingCodeReturnsNull() {
        listOf("030 123456", "(030) 123456", "", "+", "+0", "+049 30 123456", "+999", "+999 123456", "49 30 123456").forEach {
            assertNull(callingCodeCountryForPhoneNumber(it), it)
        }
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
