package com.kroegerama.kmp.kaiteki

import com.vanniktech.locale.Country
import com.vanniktech.locale.Language
import com.vanniktech.locale.Locale

public fun main() {
    Language.GERMAN
    Country.GERMANY
    Locale.fromOrNull("de-DE")
    Locale(
        Language.GERMAN,
        Country.GERMANY
    )
}