package com.kroegerama.kmp.kaiteki.locale

import kotlin.test.Test
import kotlin.test.expect

class LocalesTest {

    @Test
    fun localesLanguageTest() {
        expect("en") { Locales.ENGLISH.language }
        expect("fr") { Locales.FRENCH.language }
        expect("de") { Locales.GERMAN.language }
        expect("it") { Locales.ITALIAN.language }
        expect("ja") { Locales.JAPANESE.language }
        expect("ko") { Locales.KOREAN.language }
        expect("zh") { Locales.CHINESE.language }
        expect("zh") { Locales.SIMPLIFIED_CHINESE.language }
        expect("zh") { Locales.TRADITIONAL_CHINESE.language }
    }

    @Test
    fun localesLanguageOnlyHaveNoCountryTest() {
        expect("") { Locales.ENGLISH.country }
        expect("") { Locales.FRENCH.country }
        expect("") { Locales.GERMAN.country }
        expect("") { Locales.ITALIAN.country }
        expect("") { Locales.JAPANESE.country }
        expect("") { Locales.KOREAN.country }
        expect("") { Locales.CHINESE.country }
    }

    @Test
    fun localesCountryTest() {
        expect("FR") { Locales.FRANCE.country }
        expect("DE") { Locales.GERMANY.country }
        expect("IT") { Locales.ITALY.country }
        expect("JP") { Locales.JAPAN.country }
        expect("KR") { Locales.KOREA.country }
        expect("GB") { Locales.UK.country }
        expect("US") { Locales.US.country }
        expect("CA") { Locales.CANADA.country }
        expect("CA") { Locales.CANADA_FRENCH.country }
    }

    @Test
    fun localesCountryLanguageCombinationTest() {
        expect("fr") { Locales.FRANCE.language }
        expect("de") { Locales.GERMANY.language }
        expect("en") { Locales.UK.language }
        expect("en") { Locales.US.language }
        expect("en") { Locales.CANADA.language }
        expect("fr") { Locales.CANADA_FRENCH.language }
    }

    @Test
    fun localesRootTest() {
        expect("") { Locales.ROOT.language }
        expect("") { Locales.ROOT.country }
    }

    @Test
    fun localesChineseVariantsTest() {
        expect("CN") { Locales.SIMPLIFIED_CHINESE.country }
        expect("TW") { Locales.TRADITIONAL_CHINESE.country }
    }
}
