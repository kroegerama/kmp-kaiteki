package com.kroegerama.kmp.kaiteki.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AcceptLanguageTest {

    @Test
    fun scriptTagGetsScriptAndLanguageFallbacks() {
        assertEquals("zh-Hant-TW,zh-Hant;q=0.9,zh;q=0.8", buildAcceptLanguage(listOf("zh-Hant-TW")))
    }

    @Test
    fun sameScriptTagsShareFallbacks() {
        assertEquals(
            "zh-Hant-TW,zh-Hant-HK;q=0.9,zh-Hant;q=0.8,zh;q=0.7",
            buildAcceptLanguage(listOf("zh-Hant-TW", "zh-Hant-HK"))
        )
    }

    @Test
    fun differentScriptsKeepOwnScriptFallback() {
        assertEquals(
            "zh-Hant-TW,zh-Hant;q=0.9,zh-Hans-CN;q=0.8,zh-Hans;q=0.7,zh;q=0.6",
            buildAcceptLanguage(listOf("zh-Hant-TW", "zh-Hans-CN"))
        )
    }

    @Test
    fun languageScriptTagGetsOnlyLanguageFallback() {
        assertEquals("zh-Hant,zh;q=0.9", buildAcceptLanguage(listOf("zh-Hant")))
    }

    @Test
    fun scriptFallbackAlreadyInInputIsNotDuplicated() {
        assertEquals("zh-Hant-TW,zh-Hant;q=0.9,zh;q=0.8", buildAcceptLanguage(listOf("zh-Hant-TW", "zh-Hant")))
    }

    @Test
    fun regionTagsGetLanguageFallbacks() {
        assertEquals("de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7", buildAcceptLanguage(listOf("de-DE", "en-US")))
    }

    @Test
    fun languageAlreadyInInputIsNotDuplicated() {
        assertEquals("de-DE,de;q=0.9", buildAcceptLanguage(listOf("de-DE", "de")))
    }

    @Test
    fun bareLanguageHasNoFallback() {
        assertEquals("en", buildAcceptLanguage(listOf("en")))
    }

    @Test
    fun emptyInputYieldsNull() {
        assertNull(buildAcceptLanguage(emptyList()))
    }

    @Test
    fun blankTagYieldsNull() {
        assertNull(buildAcceptLanguage(listOf("")))
    }

    @Test
    fun emptyLanguageSubtagYieldsNull() {
        assertNull(buildAcceptLanguage(listOf("-US")))
    }

    @Test
    fun onlyLanguageIsLowercased() {
        assertEquals("en-us,en;q=0.9", buildAcceptLanguage(listOf("EN-us")))
    }

    @Test
    fun qValueIsClampedToOneTenth() {
        assertEquals(
            "zh-Hant-TW,zh-Hant;q=0.9,zh;q=0.8,sr-Latn-RS;q=0.7,sr-Latn;q=0.6,sr;q=0.5," +
                "uz-Cyrl-UZ;q=0.4,uz-Cyrl;q=0.3,uz;q=0.2,az-Arab-IR;q=0.1,az-Arab;q=0.1,az;q=0.1",
            buildAcceptLanguage(listOf("zh-Hant-TW", "sr-Latn-RS", "uz-Cyrl-UZ", "az-Arab-IR"))
        )
    }

    @Test
    fun duplicatesAreDroppedAndAtMostFourTagsAreUsed() {
        assertEquals(
            "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7,fr-FR;q=0.6,fr;q=0.5,it-IT;q=0.4,it;q=0.3",
            buildAcceptLanguage(listOf("de-DE", "de-DE", "en-US", "fr-FR", "it-IT", "es-ES"))
        )
    }
}
