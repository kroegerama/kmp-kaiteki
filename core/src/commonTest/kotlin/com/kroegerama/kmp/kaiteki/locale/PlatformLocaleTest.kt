package com.kroegerama.kmp.kaiteki.locale

import com.kroegerama.kmp.kaiteki.RobolectricTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.expect

class PlatformLocaleTest : RobolectricTest() {

    @Test
    fun createPlatformLocaleTest() {
        val locale = createPlatformLocale("de-DE")
        expect("de") { locale.language }
        expect("DE") { locale.country }
    }

    @Test
    fun currentLocaleHasLanguageTag() {
        val locale = currentPlatformLocale()
        assertTrue(locale.languageTag.isNotEmpty())
        assertTrue(locale.language.isNotEmpty())
    }

    @Test
    fun createLocaleLanguageOnly() {
        val locale = createPlatformLocale("de")
        assertEquals("de", locale.language)
        assertEquals("", locale.country)
        assertEquals("", locale.script)
    }

    @Test
    fun createLocaleLanguageAndCountry() {
        val locale = createPlatformLocale("de-DE")
        assertEquals("de", locale.language)
        assertEquals("DE", locale.country)
        assertEquals("", locale.script)
        assertEquals("", locale.variant)
    }

    @Test
    fun createLocaleWithScript() {
        val locale = createPlatformLocale("sr-Latn-RS")
        assertEquals("sr", locale.language)
        assertEquals("RS", locale.country)
        assertEquals("Latn", locale.script)
    }

    @Test
    fun createLocaleWithVariant() {
        val locale = createPlatformLocale("ca-ES-valencia")
        assertEquals("ca", locale.language)
        assertEquals("ES", locale.country)
        assertEquals("valencia", locale.variant.lowercase())
    }

    @Test
    fun displayNameInEnglish() {
        val displayLocale = createPlatformLocale("en-US")
        val german = createPlatformLocale("de-DE")
        assertContains(german.getDisplayName(displayLocale), "German")
    }

    @Test
    fun displayLanguageInEnglish() {
        val displayLocale = createPlatformLocale("en-US")
        val arabic = createPlatformLocale("ar")
        assertContains(arabic.getDisplayLanguage(displayLocale), "Arabic")
    }

    @Test
    fun displayCountryInEnglish() {
        val displayLocale = createPlatformLocale("en-US")
        val locale = createPlatformLocale("de-DE")
        assertContains(locale.getDisplayCountry(displayLocale), "Germany")
    }

    @Test
    fun displayScriptInEnglish() {
        val displayLocale = createPlatformLocale("en-US")
        val locale = createPlatformLocale("sr-Latn-RS")
        assertContains(locale.getDisplayScript(displayLocale), "Latin")
    }

    @Test
    fun displayVariantInEnglish() {
        val displayLocale = createPlatformLocale("en-US")
        val locale = createPlatformLocale("ca-ES-valencia")
        assertContains(locale.getDisplayVariant(displayLocale), "Valencia", ignoreCase = true)
    }

    @Test
    fun languageTagRoundTrip() {
        val locale = createPlatformLocale("de-DE")
        assertEquals("de-DE", locale.languageTag)
    }
}
