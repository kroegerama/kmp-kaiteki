package com.kroegerama.kmp.kaiteki.locale

import java.util.Locale

public actual typealias PlatformLocale = Locale

public actual fun currentPlatformLocale(): PlatformLocale = Locale.getDefault()

public actual fun createPlatformLocale(languageTag: String): PlatformLocale = Locale.forLanguageTag(languageTag)

public actual val PlatformLocale.languageTag: String get() = toLanguageTag()

public actual val PlatformLocale.language: String get() = language
public actual val PlatformLocale.country: String get() = country
public actual val PlatformLocale.script: String get() = script
public actual val PlatformLocale.variant: String get() = variant

public actual fun PlatformLocale.getDisplayName(locale: PlatformLocale): String = getDisplayName(locale)
public actual fun PlatformLocale.getDisplayLanguage(locale: PlatformLocale): String = getDisplayLanguage(locale)
public actual fun PlatformLocale.getDisplayCountry(locale: PlatformLocale): String = getDisplayCountry(locale)
public actual fun PlatformLocale.getDisplayScript(locale: PlatformLocale): String = getDisplayScript(locale)
public actual fun PlatformLocale.getDisplayVariant(locale: PlatformLocale): String = getDisplayVariant(locale)
