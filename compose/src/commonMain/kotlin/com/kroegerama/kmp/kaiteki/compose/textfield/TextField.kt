package com.kroegerama.kmp.kaiteki.compose.textfield

import androidx.compose.foundation.text.input.TextFieldState

public val TextFieldState.string: String get() = text.toString()
