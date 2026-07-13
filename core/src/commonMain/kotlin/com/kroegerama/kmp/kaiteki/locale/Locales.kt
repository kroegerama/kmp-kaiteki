package com.kroegerama.kmp.kaiteki.locale

/**
 * Common set of predefined [PlatformLocale] constants, mirroring the well-known locales from
 * `java.util.Locale`. Language-only entries (e.g. [GERMAN]) carry no country; country entries
 * (e.g. [GERMANY]) do.
 */
public expect object Locales {
    public val ENGLISH: PlatformLocale
    public val FRENCH: PlatformLocale
    public val GERMAN: PlatformLocale
    public val ITALIAN: PlatformLocale
    public val JAPANESE: PlatformLocale
    public val KOREAN: PlatformLocale
    public val CHINESE: PlatformLocale
    public val SIMPLIFIED_CHINESE: PlatformLocale
    public val TRADITIONAL_CHINESE: PlatformLocale
    public val FRANCE: PlatformLocale
    public val GERMANY: PlatformLocale
    public val ITALY: PlatformLocale
    public val JAPAN: PlatformLocale
    public val KOREA: PlatformLocale
    public val UK: PlatformLocale
    public val US: PlatformLocale
    public val CANADA: PlatformLocale
    public val CANADA_FRENCH: PlatformLocale
    public val ROOT: PlatformLocale
}
