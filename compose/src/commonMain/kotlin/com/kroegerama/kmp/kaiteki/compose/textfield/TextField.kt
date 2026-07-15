package com.kroegerama.kmp.kaiteki.compose.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd

/** Current text of this [TextFieldState] as a [String]. */
public val TextFieldState.string: String get() = text.toString()

/** Trims leading and trailing whitespace from the text and places the cursor at the end. */
public fun TextFieldState.trim(): Unit = setTextAndPlaceCursorAtEnd(
    text.trim().toString()
)
