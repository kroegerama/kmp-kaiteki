package com.kroegerama.kmp.kaiteki.locale

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import kotlin.test.Test
import kotlin.test.assertEquals

class CallingCodesLibPhoneNumberTest {

    @Test
    fun tableMatchesLibPhoneNumber() {
        val util = PhoneNumberUtil.getInstance()
        val expected = util.supportedRegions.associateWith(util::getCountryCodeForRegion)
        val actual = callingCodeCountries.associate { it.isoCode to it.callingCode }

        assertEquals(emptySet(), expected.keys - actual.keys, "Regions missing from callingCodeCountries")
        assertEquals(emptySet(), actual.keys - expected.keys, "Regions unknown to libphonenumber")
        val mismatches = expected.filter { (isoCode, code) -> isoCode in actual && actual[isoCode] != code }
        assertEquals(emptyMap(), mismatches, "Calling codes that differ from libphonenumber (libphonenumber values shown)")
    }

    @Test
    fun storedAreaCodesMatchLibPhoneNumber() {
        val util = PhoneNumberUtil.getInstance()
        val mismatches = regionPrefixTable.getValue(1).mapNotNull { (areaCode, isoCode) ->
            // Some regions only accept certain subscriber numbers, so the subscriber parts of their example numbers are tried as well.
            val subscriberNumbers = PhoneNumberType.entries
                .mapNotNull { util.getExampleNumberForType(isoCode, it) }
                .map { util.getNationalSignificantNumber(it).substring(3) }
                .plus("2345678")
            val regions = subscriberNumbers.mapNotNull { subscriberNumber ->
                util.getRegionCodeForNumber(PhoneNumber().setCountryCode(1).setNationalNumber("$areaCode$subscriberNumber".toLong()))
            }.toSet()
            if (regions == setOf(isoCode)) null else Triple(areaCode, isoCode, regions)
        }
        assertEquals(emptyList(), mismatches, "Area codes that libphonenumber assigns to other regions (area code, stored, libphonenumber)")
    }

    @Test
    fun exampleNumbersOfPrefixRegionsResolve() {
        val util = PhoneNumberUtil.getInstance()
        val regions = regionPrefixTable.values.flatMap { it.values }.toSet()
        val actual = regions.associateWith { callingCodeCountryForPhoneNumber(util.exampleE164(it))?.isoCode }
        assertEquals(regions.associateWith { it }, actual)
    }

    @Test
    fun exampleNumbersResolveToOwnOrMainRegion() {
        val util = PhoneNumberUtil.getInstance()
        val mismatches = util.supportedRegions.mapNotNull { isoCode ->
            val callingCode = util.getCountryCodeForRegion(isoCode)
            val mainRegion = util.getRegionCodeForCountryCode(callingCode)
            val allowed = setOf(CallingCodeCountry(isoCode, callingCode), CallingCodeCountry(mainRegion, callingCode))
            val actual = callingCodeCountryForPhoneNumber(util.exampleE164(isoCode))
            if (actual in allowed) null else isoCode to actual
        }
        assertEquals(emptyList(), mismatches, "Regions whose example number resolves to an unrelated region")
    }

    private fun PhoneNumberUtil.exampleE164(isoCode: String): String = format(getExampleNumber(isoCode), PhoneNumberFormat.E164)
}
