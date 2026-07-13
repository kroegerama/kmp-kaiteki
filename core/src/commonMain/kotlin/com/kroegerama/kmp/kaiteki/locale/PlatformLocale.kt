package com.kroegerama.kmp.kaiteki.locale

/**
 * Multiplatform locale handle (`java.util.Locale` on Android/JVM, `NSLocale` on Apple).
 *
 * Create instances with [createPlatformLocale], obtain the user's locale with
 * [currentPlatformLocale], or use the constants in [Locales].
 */
public expect class PlatformLocale

/** The current default locale of the device or user. */
public expect fun currentPlatformLocale(): PlatformLocale

/** Creates a locale from an IETF BCP 47 language tag, e.g. `"de-DE"` or `"zh-Hant-TW"`. */
public expect fun createPlatformLocale(languageTag: String): PlatformLocale

/** This locale as an IETF BCP 47 language tag. */
public expect val PlatformLocale.languageTag: String

/** ISO 639 language code, e.g. `"de"`. */
public expect val PlatformLocale.language: String

/** ISO 3166 country/region code, e.g. `"DE"`, or empty if unset. */
public expect val PlatformLocale.country: String

/** ISO 15924 script code, e.g. `"Hant"`, or empty if unset. */
public expect val PlatformLocale.script: String

/** Variant subtag, or empty if unset. */
public expect val PlatformLocale.variant: String

/** Full name of this locale, translated into [locale]. */
public expect fun PlatformLocale.getDisplayName(locale: PlatformLocale = currentPlatformLocale()): String

/** Name of this locale's language, translated into [locale]. */
public expect fun PlatformLocale.getDisplayLanguage(locale: PlatformLocale = currentPlatformLocale()): String

/** Name of this locale's country, translated into [locale]. */
public expect fun PlatformLocale.getDisplayCountry(locale: PlatformLocale = currentPlatformLocale()): String

/** Name of this locale's script, translated into [locale]. */
public expect fun PlatformLocale.getDisplayScript(locale: PlatformLocale = currentPlatformLocale()): String

/** Name of this locale's variant, translated into [locale]. */
public expect fun PlatformLocale.getDisplayVariant(locale: PlatformLocale = currentPlatformLocale()): String

/** Full name of this locale in the current default locale. */
public val PlatformLocale.displayName: String get() = getDisplayName()

/** Language name of this locale in the current default locale. */
public val PlatformLocale.displayLanguage: String get() = getDisplayLanguage()

/** Country name of this locale in the current default locale. */
public val PlatformLocale.displayCountry: String get() = getDisplayCountry()

/** Script name of this locale in the current default locale. */
public val PlatformLocale.displayScript: String get() = getDisplayScript()

/** Variant name of this locale in the current default locale. */
public val PlatformLocale.displayVariant: String get() = getDisplayVariant()
