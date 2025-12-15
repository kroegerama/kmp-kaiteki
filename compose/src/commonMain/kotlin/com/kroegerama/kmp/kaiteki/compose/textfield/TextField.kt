package com.kroegerama.kmp.kaiteki.compose.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd

public val TextFieldState.string: String get() = text.toString()

public fun TextFieldState.trim(): Unit = setTextAndPlaceCursorAtEnd(
    text.trim().toString()
)
