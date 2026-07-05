package com.kroegerama.kmp.kaiteki.formatting

import com.kroegerama.kmp.kaiteki.locale.Locales
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.expect

class DefaultDecimalFormatterTest {

    private val formatterDE = DefaultDecimalFormatter(
        locale = Locales.GERMANY
    )

    private val formatterUS = DefaultDecimalFormatter(
        locale = Locales.US
    )

    private val formatterPlain = DefaultDecimalFormatter(
        locale = Locales.US,
        isGroupingUsed = false,
        maximumFractionDigits = 15,
        maximumIntegerDigits = 310,
    )

    @Test
    fun testDE() {
        listOf(
            1 to "1",
            2f to "2",
            3.0 to "3",
            10_000.123f to "10.000,123",
            20_000.3456789 to "20.000,346",
            // above 2^53 — must not round-trip through Double
            9_007_199_254_740_993L to "9.007.199.254.740.993",
            Long.MAX_VALUE to "9.223.372.036.854.775.807",
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
            // above 2^53 — must not round-trip through Double
            9_007_199_254_740_993L to "9,007,199,254,740,993",
            Long.MAX_VALUE to "9,223,372,036,854,775,807",
        ).forEach { (value, expected) ->
            expect(expected) { formatterUS.format(value) }
        }
    }

    @Test
    fun testTypes() {
        listOf(
            Byte.MIN_VALUE to "-128",
            Byte.MAX_VALUE to "127",
            Short.MIN_VALUE to "-32768",
            Short.MAX_VALUE to "32767",
            Int.MIN_VALUE to "-2147483648",
            Int.MAX_VALUE to "2147483647",
            Long.MIN_VALUE to "-9223372036854775808",
            Long.MAX_VALUE to "9223372036854775807",
            -Float.MAX_VALUE to "-34028234663852886".padEnd(40, '0'),
            Float.MAX_VALUE to "34028234663852886".padEnd(39, '0'),
        ).forEach { (value, expected) ->
            expect(expected) { formatterPlain.format(value) }
        }
    }

    @Test
    fun testNonFinite() {
        // Apple prefixes positive infinity with "+" by default, the JVM does not; accept both.
        listOf(
            Double.NaN to setOf("NaN"),
            Double.POSITIVE_INFINITY to setOf("∞", "+∞"),
            Double.NEGATIVE_INFINITY to setOf("-∞"),
            Float.NaN to setOf("NaN"),
            Float.POSITIVE_INFINITY to setOf("∞", "+∞"),
            Float.NEGATIVE_INFINITY to setOf("-∞"),
        ).forEach { (value, accepted) ->
            assertContains(accepted, formatterUS.format(value))
        }
    }
}
