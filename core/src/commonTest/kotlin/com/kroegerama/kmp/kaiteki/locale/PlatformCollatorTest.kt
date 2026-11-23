package com.kroegerama.kmp.kaiteki.locale

import com.kroegerama.kmp.kaiteki.RobolectricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PlatformCollatorTest : RobolectricTest() {

    @Test
    fun germanSortsUmlautsWithBaseLetters() {
        val collator = createPlatformCollator(createPlatformLocale("de-DE"))
        val sorted = listOf("Zypern", "Österreich", "Deutschland", "Ägypten", "Albanien").sortedWith(collator)
        assertEquals(listOf("Ägypten", "Albanien", "Deutschland", "Österreich", "Zypern"), sorted)
    }

    @Test
    fun swedishSortsUmlautAfterZ() {
        val swedish = createPlatformCollator(createPlatformLocale("sv-SE"))
        assertTrue(swedish.compare("Österrike", "Zambia") > 0)

        val german = createPlatformCollator(createPlatformLocale("de-DE"))
        assertTrue(german.compare("Österreich", "Zypern") < 0)
    }

    @Test
    fun identicalStringsAreEqual() {
        val collator = createPlatformCollator(createPlatformLocale("de-DE"))
        assertEquals(0, collator.compare("Apfel", "Apfel"))
    }

    @Test
    fun caseDifferenceIsNotEqual() {
        val collator = createPlatformCollator(createPlatformLocale("de-DE"))
        assertNotEquals(0, collator.compare("apfel", "Apfel"))
    }

    @Test
    fun defaultLocaleOrdersBasicLatin() {
        val collator = createPlatformCollator()
        assertTrue(collator.compare("a", "b") < 0)
    }
}
