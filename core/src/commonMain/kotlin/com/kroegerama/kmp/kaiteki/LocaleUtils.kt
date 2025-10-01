package com.kroegerama.kmp.kaiteki

public fun isoCountryCodeToFlag(isoCode: String): String {
    require(isoCode.length == 2) { "Country code must be exactly 2 letters" }

    val upper = isoCode.uppercase()
    val base = 0x1F1E6
    val offset = 'A'.code

    return StringBuilder(4).apply {
        upper.forEach { ch ->
            require(ch in 'A'..'Z') { "Invalid character in country code: $ch" }
            val cp = base + (ch.code - offset)
            val code = cp - 0x10000
            val high = ((code shr 10) + 0xD800).toChar()
            val low = ((code and 0x3FF) + 0xDC00).toChar()
            append(high)
            append(low)
        }
    }.toString()
}
