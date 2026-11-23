package com.kroegerama.kmp.kaiteki.locale

import com.google.i18n.phonenumbers.PhoneNumberUtil
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
}
