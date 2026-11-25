package com.kroegerama.kmp.kaiteki.locale

/**
 * A region together with its international calling code.
 *
 * @property isoCode Uppercase two-letter CLDR region code (ISO 3166-1 alpha-2 plus a few codes such as `"XK"`), e.g. `"DE"`.
 * @property callingCode International calling code without the leading `+`, e.g. `49`.
 */
public data class CallingCodeCountry(
    val isoCode: String,
    val callingCode: Int
)

/** Every region libphonenumber supports, sorted by [CallingCodeCountry.isoCode]. */
public val callingCodeCountries: List<CallingCodeCountry> by lazy {
    callingCodeTable.flatMap { (callingCode, isoCodes) ->
        isoCodes.map { isoCode -> CallingCodeCountry(isoCode, callingCode) }
    }.sortedBy { it.isoCode }
}

private val callingCodeByIsoCode: Map<String, Int> by lazy {
    callingCodeCountries.associate { it.isoCode to it.callingCode }
}

/**
 * Returns the international calling code for a two-letter CLDR region code.
 *
 * @param isoCode A two-letter region code (ISO 3166-1 alpha-2 plus a few codes such as "XK"), e.g. "DE". Case-insensitive.
 * @return The calling code without the leading `+` (e.g. `49`), or `null` if [isoCode] is unknown or invalid.
 */
public fun isoCountryCodeToCallingCode(isoCode: String): Int? {
    if (isoCode.length != 2 || !isoCode.all { it in 'A'..'Z' || it in 'a'..'z' }) return null
    return callingCodeByIsoCode[isoCode.uppercase()]
}

/**
 * The international calling code of this locale's country, or `null` when the locale has no country or its region has no calling code.
 * For [currentPlatformLocale] the country comes from the preferred language tag (`en-GB` gives `44`), not from the device region.
 *
 * @see isoCountryCodeToCallingCode
 */
public val PlatformLocale.callingCode: Int?
    get() = isoCountryCodeToCallingCode(country)

/**
 * Resolves the region of a phone number in international format, including regions that share a calling code such as `+1 242` (Bahamas).
 * The result is best effort: numbers with an unknown prefix resolve to the main region of their calling code.
 *
 * @param phoneNumber A number starting with `+` or `00`, e.g. `"+1 (242) 555-0100"`. A `(0)` group and non-digit characters are ignored.
 * @return The region with its calling code, or `null` if [phoneNumber] is not in international format or has no known calling code.
 */
public fun callingCodeCountryForPhoneNumber(phoneNumber: String): CallingCodeCountry? {
    val start = phoneNumber.indexOfFirst { it == '+' || it in '0'..'9' }
    if (start < 0) return null
    val international = when {
        phoneNumber[start] == '+' ->phoneNumber.substring(start + 1)
        phoneNumber.startsWith("00", start) -> phoneNumber.substring(start + 2)
        else -> return null
    }
    val digits = international.replace("(0)", "").filter { it in '0'..'9' }
    if (digits.startsWith('0')) return null
    for (length in 1..minOf(3, digits.length)) {
        val callingCode = digits.substring(0, length).toInt()
        val isoCodes = callingCodeTable[callingCode] ?: continue
        val mainRegion = isoCodes.first()
        if (isoCodes.size == 1) return CallingCodeCountry(mainRegion, callingCode)
        val nationalNumber = digits.substring(length)
        val prefixes = regionPrefixTable[callingCode].orEmpty()
        val longestPrefix = minOf(nationalNumber.length, MAX_REGION_PREFIX_LENGTH)
        val isoCode = (longestPrefix downTo 1).firstNotNullOfOrNull { prefixes[nationalNumber.substring(0, it)] } ?: mainRegion
        return CallingCodeCountry(isoCode, callingCode)
    }
    return null
}

internal const val MAX_REGION_PREFIX_LENGTH: Int = 5

private fun regionPrefixes(vararg regions: Pair<String, List<String>>): Map<String, String> =
    regions.flatMap { (isoCode, prefixes) -> prefixes.map { prefix -> prefix to isoCode } }.toMap()

// Calling code to national number prefix to region, for regions that share a calling code with a main region.
// Derived from google/libphonenumber phone number metadata.
// Copyright The Libphonenumber Authors, licensed under the Apache License, Version 2.0.
internal val regionPrefixTable: Map<Int, Map<String, String>> by lazy {
    mapOf(
        1 to regionPrefixes(
            "AG" to listOf("268"),
            "AI" to listOf("264"),
            "AS" to listOf("684"),
            "BB" to listOf("246"),
            "BM" to listOf("441"),
            "BS" to listOf("242"),
            "CA" to listOf(
                "204", "226", "236", "249", "250", "257", "263", "273", "289", "306", "343", "354", "365", "367", "368", "382", "403", "416",
                "418", "428", "431", "437", "438", "450", "468", "474", "506", "514", "519", "548", "579", "581", "584", "587", "600", "604",
                "613", "622", "633", "639", "647", "672", "683", "705", "709", "742", "753", "778", "780", "782", "807", "819", "825", "867",
                "873", "879", "902", "905", "942"
            ),
            "DM" to listOf("767"),
            "DO" to listOf("809", "829", "849"),
            "GD" to listOf("473"),
            "GU" to listOf("671"),
            "JM" to listOf("658", "876"),
            "KN" to listOf("869"),
            "KY" to listOf("345"),
            "LC" to listOf("758"),
            "MP" to listOf("670"),
            "MS" to listOf("664"),
            "PR" to listOf("787", "939"),
            "SX" to listOf("721"),
            "TC" to listOf("649"),
            "TT" to listOf("868"),
            "VC" to listOf("784"),
            "VG" to listOf("284"),
            "VI" to listOf("340"),
        ),
        7 to regionPrefixes("KZ" to listOf("7")),
        39 to regionPrefixes("VA" to listOf("06698")),
        44 to regionPrefixes("IM" to listOf("1624", "7524", "7624", "74576")),
        47 to regionPrefixes("SJ" to listOf("79")),
        290 to regionPrefixes("TA" to listOf("8")),
        358 to regionPrefixes("AX" to listOf("18")),
        599 to regionPrefixes("BQ" to listOf("3", "4", "7")),
    )
}

// Derived from google/libphonenumber CountryCodeToRegionCodeMap.java, without the non-geographic region "001".
// Copyright The Libphonenumber Authors, licensed under the Apache License, Version 2.0.
private val callingCodeTable: Map<Int, List<String>> = mapOf(
    1 to listOf(
        "US", "AG", "AI", "AS", "BB", "BM", "BS", "CA", "DM", "DO", "GD", "GU", "JM", "KN", "KY", "LC", "MP", "MS", "PR", "SX", "TC", "TT", "VC",
        "VG", "VI"
    ),
    7 to listOf("RU", "KZ"),
    20 to listOf("EG"),
    27 to listOf("ZA"),
    30 to listOf("GR"),
    31 to listOf("NL"),
    32 to listOf("BE"),
    33 to listOf("FR"),
    34 to listOf("ES"),
    36 to listOf("HU"),
    39 to listOf("IT", "VA"),
    40 to listOf("RO"),
    41 to listOf("CH"),
    43 to listOf("AT"),
    44 to listOf("GB", "GG", "IM", "JE"),
    45 to listOf("DK"),
    46 to listOf("SE"),
    47 to listOf("NO", "SJ"),
    48 to listOf("PL"),
    49 to listOf("DE"),
    51 to listOf("PE"),
    52 to listOf("MX"),
    53 to listOf("CU"),
    54 to listOf("AR"),
    55 to listOf("BR"),
    56 to listOf("CL"),
    57 to listOf("CO"),
    58 to listOf("VE"),
    60 to listOf("MY"),
    61 to listOf("AU", "CC", "CX"),
    62 to listOf("ID"),
    63 to listOf("PH"),
    64 to listOf("NZ"),
    65 to listOf("SG"),
    66 to listOf("TH"),
    81 to listOf("JP"),
    82 to listOf("KR"),
    84 to listOf("VN"),
    86 to listOf("CN"),
    90 to listOf("TR"),
    91 to listOf("IN"),
    92 to listOf("PK"),
    93 to listOf("AF"),
    94 to listOf("LK"),
    95 to listOf("MM"),
    98 to listOf("IR"),
    211 to listOf("SS"),
    212 to listOf("MA", "EH"),
    213 to listOf("DZ"),
    216 to listOf("TN"),
    218 to listOf("LY"),
    220 to listOf("GM"),
    221 to listOf("SN"),
    222 to listOf("MR"),
    223 to listOf("ML"),
    224 to listOf("GN"),
    225 to listOf("CI"),
    226 to listOf("BF"),
    227 to listOf("NE"),
    228 to listOf("TG"),
    229 to listOf("BJ"),
    230 to listOf("MU"),
    231 to listOf("LR"),
    232 to listOf("SL"),
    233 to listOf("GH"),
    234 to listOf("NG"),
    235 to listOf("TD"),
    236 to listOf("CF"),
    237 to listOf("CM"),
    238 to listOf("CV"),
    239 to listOf("ST"),
    240 to listOf("GQ"),
    241 to listOf("GA"),
    242 to listOf("CG"),
    243 to listOf("CD"),
    244 to listOf("AO"),
    245 to listOf("GW"),
    246 to listOf("IO"),
    247 to listOf("AC"),
    248 to listOf("SC"),
    249 to listOf("SD"),
    250 to listOf("RW"),
    251 to listOf("ET"),
    252 to listOf("SO"),
    253 to listOf("DJ"),
    254 to listOf("KE"),
    255 to listOf("TZ"),
    256 to listOf("UG"),
    257 to listOf("BI"),
    258 to listOf("MZ"),
    260 to listOf("ZM"),
    261 to listOf("MG"),
    262 to listOf("RE", "YT"),
    263 to listOf("ZW"),
    264 to listOf("NA"),
    265 to listOf("MW"),
    266 to listOf("LS"),
    267 to listOf("BW"),
    268 to listOf("SZ"),
    269 to listOf("KM"),
    290 to listOf("SH", "TA"),
    291 to listOf("ER"),
    297 to listOf("AW"),
    298 to listOf("FO"),
    299 to listOf("GL"),
    350 to listOf("GI"),
    351 to listOf("PT"),
    352 to listOf("LU"),
    353 to listOf("IE"),
    354 to listOf("IS"),
    355 to listOf("AL"),
    356 to listOf("MT"),
    357 to listOf("CY"),
    358 to listOf("FI", "AX"),
    359 to listOf("BG"),
    370 to listOf("LT"),
    371 to listOf("LV"),
    372 to listOf("EE"),
    373 to listOf("MD"),
    374 to listOf("AM"),
    375 to listOf("BY"),
    376 to listOf("AD"),
    377 to listOf("MC"),
    378 to listOf("SM"),
    380 to listOf("UA"),
    381 to listOf("RS"),
    382 to listOf("ME"),
    383 to listOf("XK"),
    385 to listOf("HR"),
    386 to listOf("SI"),
    387 to listOf("BA"),
    389 to listOf("MK"),
    420 to listOf("CZ"),
    421 to listOf("SK"),
    423 to listOf("LI"),
    500 to listOf("FK"),
    501 to listOf("BZ"),
    502 to listOf("GT"),
    503 to listOf("SV"),
    504 to listOf("HN"),
    505 to listOf("NI"),
    506 to listOf("CR"),
    507 to listOf("PA"),
    508 to listOf("PM"),
    509 to listOf("HT"),
    590 to listOf("GP", "BL", "MF"),
    591 to listOf("BO"),
    592 to listOf("GY"),
    593 to listOf("EC"),
    594 to listOf("GF"),
    595 to listOf("PY"),
    596 to listOf("MQ"),
    597 to listOf("SR"),
    598 to listOf("UY"),
    599 to listOf("CW", "BQ"),
    670 to listOf("TL"),
    672 to listOf("NF"),
    673 to listOf("BN"),
    674 to listOf("NR"),
    675 to listOf("PG"),
    676 to listOf("TO"),
    677 to listOf("SB"),
    678 to listOf("VU"),
    679 to listOf("FJ"),
    680 to listOf("PW"),
    681 to listOf("WF"),
    682 to listOf("CK"),
    683 to listOf("NU"),
    685 to listOf("WS"),
    686 to listOf("KI"),
    687 to listOf("NC"),
    688 to listOf("TV"),
    689 to listOf("PF"),
    690 to listOf("TK"),
    691 to listOf("FM"),
    692 to listOf("MH"),
    850 to listOf("KP"),
    852 to listOf("HK"),
    853 to listOf("MO"),
    855 to listOf("KH"),
    856 to listOf("LA"),
    880 to listOf("BD"),
    886 to listOf("TW"),
    960 to listOf("MV"),
    961 to listOf("LB"),
    962 to listOf("JO"),
    963 to listOf("SY"),
    964 to listOf("IQ"),
    965 to listOf("KW"),
    966 to listOf("SA"),
    967 to listOf("YE"),
    968 to listOf("OM"),
    970 to listOf("PS"),
    971 to listOf("AE"),
    972 to listOf("IL"),
    973 to listOf("BH"),
    974 to listOf("QA"),
    975 to listOf("BT"),
    976 to listOf("MN"),
    977 to listOf("NP"),
    992 to listOf("TJ"),
    993 to listOf("TM"),
    994 to listOf("AZ"),
    995 to listOf("GE"),
    996 to listOf("KG"),
    998 to listOf("UZ"),
)
