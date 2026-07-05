package com.kroegerama.kmp.kaiteki.locale

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleIdentifier
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.NSLocaleScriptCode
import platform.Foundation.NSLocaleVariantCode
import platform.Foundation.countryCode
import platform.Foundation.languageCode
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages
import platform.Foundation.scriptCode
import platform.Foundation.variantCode

public actual typealias PlatformLocale = NSLocale

public actual fun currentPlatformLocale(): PlatformLocale {
    val language = NSLocale.preferredLanguages[0] as String
    return NSLocale(language)
}

public actual fun createPlatformLocale(languageTag: String): PlatformLocale = NSLocale(languageTag)

public actual val PlatformLocale.languageTag: String get() = localeIdentifier.substringBefore('@').replace('_', '-')

public actual val PlatformLocale.language: String get() = languageCode
public actual val PlatformLocale.country: String get() = countryCode ?: ""
public actual val PlatformLocale.script: String get() = scriptCode ?: ""
public actual val PlatformLocale.variant: String get() = variantCode ?: ""

public actual fun PlatformLocale.getDisplayName(locale: PlatformLocale): String =
    locale.displayNameForKey(NSLocaleIdentifier, localeIdentifier) ?: localeIdentifier

public actual fun PlatformLocale.getDisplayLanguage(locale: PlatformLocale): String =
    locale.displayNameForKey(NSLocaleLanguageCode, languageCode) ?: languageCode

public actual fun PlatformLocale.getDisplayCountry(locale: PlatformLocale): String = countryCode?.let {
    locale.displayNameForKey(NSLocaleCountryCode, it)
} ?: countryCode ?: ""

public actual fun PlatformLocale.getDisplayScript(locale: PlatformLocale): String = scriptCode?.let {
    locale.displayNameForKey(NSLocaleScriptCode, it)
} ?: scriptCode ?: ""

public actual fun PlatformLocale.getDisplayVariant(locale: PlatformLocale): String = variantCode?.let {
    locale.displayNameForKey(NSLocaleVariantCode, it)
} ?: variantCode ?: ""
