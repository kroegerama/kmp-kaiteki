package com.kroegerama.kmp.kaiteki.locale

/**
 * Converts a two-letter ISO 3166-1 alpha-2 country code to its corresponding Unicode regional indicator flag emoji.
 *
 * @param isoCode A two-letter country code (e.g. "DE", "US"). Case-insensitive.
 * @param fallback Value returned when [isoCode] is not a valid two-letter alphabetic code.
 * @return The flag emoji string, or [fallback] if the input is invalid.
 */
public fun isoCountryCodeToFlag(isoCode: String, fallback: String = ""): String {
    if (isoCode.length != 2) return fallback

    val upper = isoCode.uppercase()
    val base = 0x1F1E6
    val offset = 'A'.code

    return StringBuilder(4).apply {
        upper.forEach { ch ->
            if (ch !in 'A'..'Z') return fallback
            val cp = base + (ch.code - offset)
            val code = cp - 0x10000
            val high = ((code shr 10) + 0xD800).toChar()
            val low = ((code and 0x3FF) + 0xDC00).toChar()
            append(high)
            append(low)
        }
    }.toString()
}

/**
 * The regional-indicator flag emoji for this locale's country, or an empty string when the locale
 * has no country component.
 *
 * @see isoCountryCodeToFlag
 */
public expect val PlatformLocale.unicodeFlag: String
