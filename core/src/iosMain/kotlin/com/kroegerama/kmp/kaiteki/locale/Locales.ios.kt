package com.kroegerama.kmp.kaiteki.locale

import platform.Foundation.NSLocale
import platform.Foundation.systemLocale

public actual object Locales {
    public actual val ENGLISH: PlatformLocale = NSLocale("en")
    public actual val FRENCH: PlatformLocale = NSLocale("fr")
    public actual val GERMAN: PlatformLocale = NSLocale("de")
    public actual val ITALIAN: PlatformLocale = NSLocale("it")
    public actual val JAPANESE: PlatformLocale = NSLocale("ja")
    public actual val KOREAN: PlatformLocale = NSLocale("ko")
    public actual val CHINESE: PlatformLocale = NSLocale("zh")
    public actual val SIMPLIFIED_CHINESE: PlatformLocale = NSLocale("zh-CN")
    public actual val TRADITIONAL_CHINESE: PlatformLocale = NSLocale("zh-TW")
    public actual val FRANCE: PlatformLocale = NSLocale("fr-FR")
    public actual val GERMANY: PlatformLocale = NSLocale("de-DE")
    public actual val ITALY: PlatformLocale = NSLocale("it-IT")
    public actual val JAPAN: PlatformLocale = NSLocale("ja-JP")
    public actual val KOREA: PlatformLocale = NSLocale("ko-KR")
    public actual val UK: PlatformLocale = NSLocale("en-GB")
    public actual val US: PlatformLocale = NSLocale("en-US")
    public actual val CANADA: PlatformLocale = NSLocale("en-CA")
    public actual val CANADA_FRENCH: PlatformLocale = NSLocale("fr-CA")
    public actual val ROOT: PlatformLocale = NSLocale.systemLocale
}
