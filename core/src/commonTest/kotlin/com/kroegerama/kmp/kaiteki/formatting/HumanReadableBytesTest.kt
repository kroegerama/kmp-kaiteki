package com.kroegerama.kmp.kaiteki.formatting

import kotlin.test.Test
import kotlin.test.expect

class HumanReadableBytesTest {

    @Test
    fun testBinaryUnits() {
        listOf(
            0L to "0 B",
            1L to "1 B",
            1023L to "1023 B",
            1024L to "1.0 KiB",
            1536L to "1.5 KiB",
            1_048_576L to "1.0 MiB",
            1_073_741_824L to "1.0 GiB",
            1_099_511_627_776L to "1.0 TiB",
            1_125_899_906_842_624L to "1.0 PiB",
            1_152_921_504_606_846_976L to "1.0 EiB",
        ).forEach { (bytes, expected) ->
            expect(expected) { bytes.asHumanReadableBytes(si = false) }
        }
    }

    @Test
    fun testSiUnits() {
        listOf(
            0L to "0 B",
            1L to "1 B",
            999L to "999 B",
            1000L to "1.0 kB",
            1500L to "1.5 kB",
            1_000_000L to "1.0 MB",
            1_000_000_000L to "1.0 GB",
            1_000_000_000_000L to "1.0 TB",
            1_000_000_000_000_000L to "1.0 PB",
            1_000_000_000_000_000_000L to "1.0 EB",
        ).forEach { (bytes, expected) ->
            expect(expected) { bytes.asHumanReadableBytes(si = true) }
        }
    }

    @Test
    fun testDefaultIsBinary() {
        expect("1.0 KiB") { 1024L.asHumanReadableBytes() }
    }
}
