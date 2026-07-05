package com.kroegerama.kmp.kaiteki.feature

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BlurHashTest {

    @Test
    fun decodeValidBlurHash() {
        val result = BlurHash.decode("LGF5?xYk^6#M@-5c,1J5@[or[Q6.")
        assertNotNull(result)
        assertEquals(4, result.componentsX)
        assertEquals(3, result.componentsY)
    }

    @Test
    fun decodeMinimalBlurHash1x1() {
        val result = BlurHash.decode("00F5?x")
        assertNotNull(result)
        assertEquals(1, result.componentsX)
        assertEquals(1, result.componentsY)
    }

    @Test
    fun decodeMaxComponents9x9() {
        val result = BlurHash.decode("|HF5?xYk^6#M9wKSW@j=#*@-5b,1J5O[V=R:s;w[@[or[k6.O[TLtJnNnO};FxngOZE3NgNHsps,jMFxS#OtcXnzRjxZxHj]OYNeR:JCs9xunhwIbeIpNaxHNGr;v}aeo0Xmt6XS\$et6#*\$ft6nhxHnNV@w{nOenwfNHo0")
        assertNotNull(result)
        assertEquals(9, result.componentsX)
        assertEquals(9, result.componentsY)
    }

    @Test
    fun decodeReturnsNullForTooShortInput() {
        assertNull(BlurHash.decode(""))
        assertNull(BlurHash.decode("abc"))
        assertNull(BlurHash.decode("12345"))
    }

    @Test
    fun decodeReturnsNullForWrongLength() {
        // Valid prefix but wrong total length
        assertNull(BlurHash.decode("LGF5?xYk^6#M@-5c,1J5@[or[Q6.EXTRA"))
    }

    @Test
    fun decodeReturnsNullForInvalidBase83Characters() {
        val valid = "LGF5?xYk^6#M@-5c,1J5@[or[Q6."
        // '!', '(' and ' ' are not part of the Base83 alphabet; length stays valid
        assertNull(BlurHash.decode("!" + valid.drop(1))) // size flag
        assertNull(BlurHash.decode("L(" + valid.drop(2))) // max AC
        assertNull(BlurHash.decode(valid.take(3) + "!" + valid.drop(4))) // DC
        assertNull(BlurHash.decode(valid.dropLast(1) + " ")) // AC
    }

    @Test
    fun averageColorIsConsistent() {
        val result1 = BlurHash.decode("LGF5?xYk^6#M@-5c,1J5@[or[Q6.")
        val result2 = BlurHash.decode("LGF5?xYk^6#M@-5c,1J5@[or[Q6.")
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(result1.averageColor, result2.averageColor)
    }

    @Test
    fun punchAffectsFactors() {
        val noPunch = BlurHash.decode("LGF5?xYk^6#M@-5c,1J5@[or[Q6.", punch = 0.5f)
        val fullPunch = BlurHash.decode("LGF5?xYk^6#M@-5c,1J5@[or[Q6.", punch = 1f)
        assertNotNull(noPunch)
        assertNotNull(fullPunch)
        // DC component (first 3 factors) should be the same regardless of punch
        assertEquals(noPunch.factors[0], fullPunch.factors[0])
        assertEquals(noPunch.factors[1], fullPunch.factors[1])
        assertEquals(noPunch.factors[2], fullPunch.factors[2])
        // AC components should differ
        assertEquals(noPunch.componentsX, fullPunch.componentsX)
        val hasAcDifference = (3 until noPunch.factors.size).any { i ->
            noPunch.factors[i] != fullPunch.factors[i]
        }
        assertEquals(true, hasAcDifference)
    }

    @Test
    fun factorsSizeMatchesComponents() {
        val result = BlurHash.decode("LGF5?xYk^6#M@-5c,1J5@[or[Q6.")
        assertNotNull(result)
        assertEquals(result.componentsX * result.componentsY * 3, result.factors.size)
    }
}
