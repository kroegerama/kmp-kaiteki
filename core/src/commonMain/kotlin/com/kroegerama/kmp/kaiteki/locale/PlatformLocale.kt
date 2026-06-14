package com.kroegerama.kmp.kaiteki.locale

public expect class PlatformLocale

public expect fun currentPlatformLocale(): PlatformLocale
public expect fun createPlatformLocale(languageTag: String): PlatformLocale

public expect val PlatformLocale.languageTag: String

public expect val PlatformLocale.language: String
public expect val PlatformLocale.country: String
public expect val PlatformLocale.script: String
public expect val PlatformLocale.variant: String

public expect fun PlatformLocale.getDisplayName(locale: PlatformLocale = currentPlatformLocale()): String
public expect fun PlatformLocale.getDisplayLanguage(locale: PlatformLocale = currentPlatformLocale()): String
public expect fun PlatformLocale.getDisplayCountry(locale: PlatformLocale = currentPlatformLocale()): String
public expect fun PlatformLocale.getDisplayScript(locale: PlatformLocale = currentPlatformLocale()): String
public expect fun PlatformLocale.getDisplayVariant(locale: PlatformLocale = currentPlatformLocale()): String

public val PlatformLocale.displayName: String get() = getDisplayName()
public val PlatformLocale.displayLanguage: String get() = getDisplayLanguage()
public val PlatformLocale.displayCountry: String get() = getDisplayCountry()
public val PlatformLocale.displayScript: String get() = getDisplayScript()
public val PlatformLocale.displayVariant: String get() = getDisplayVariant()
