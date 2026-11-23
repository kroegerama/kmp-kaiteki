package com.kroegerama.kmp.kaiteki.locale

/**
 * Creates a locale-aware string comparator, backed by ICU on Android and JVM and by Foundation on Apple.
 *
 * @param locale the locale whose collation rules are applied.
 * @return a comparator ordering strings according to [locale]. The order of punctuation and case ties can differ between platforms,
 *   and distinct strings can compare as equal, so it is unsuitable as the key order of sorted sets or maps.
 */
public expect fun createPlatformCollator(
    locale: PlatformLocale = currentPlatformLocale(),
): Comparator<String>
