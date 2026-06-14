package com.kroegerama.kmp.kaiteki.formatting

import com.kroegerama.kmp.kaiteki.locale.Locales
import kotlin.test.Test
import kotlin.test.expect

class DefaultDecimalFormatterTest {

    private val formatterDE = DefaultDecimalFormatter(
        locale = Locales.GERMANY
    )

    private val formatterUS = DefaultDecimalFormatter(
        locale = Locales.US
    )

    @Test
    fun testDE() {
        listOf(
            1 to "1",
            2f to "2",
            3.0 to "3",
            10_000.123f to "10.000,123",
            20_000.3456789 to "20.000,346",
        ).forEach { (value, expected) ->
            expect(expected) { formatterDE.format(value) }
        }
    }

    @Test
    fun testUS() {
        listOf(
            1 to "1",
            2f to "2",
            3.0 to "3",
            10_000.123f to "10,000.123",
            20_000.3456789 to "20,000.346",
        ).forEach { (value, expected) ->
            expect(expected) { formatterUS.format(value) }
        }
    }
}
