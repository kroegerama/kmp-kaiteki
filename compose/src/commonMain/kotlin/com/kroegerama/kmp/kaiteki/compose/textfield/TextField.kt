package com.kroegerama.kmp.kaiteki.compose.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow

/** Current text of this [TextFieldState] as a [String]. */
public val TextFieldState.string: String get() = text.toString()

/**
 * Emits the current text whenever its characters change, ignoring cursor, selection and IME composition updates.
 *
 * @return a [Flow] backed by [snapshotFlow], so it emits the current text on collection and skips unchanged values.
 */
public fun TextFieldState.textAsFlow(): Flow<String> = snapshotFlow { string }

/** Trims leading and trailing whitespace from the text and places the cursor at the end. */
public fun TextFieldState.trim(): Unit = setTextAndPlaceCursorAtEnd(
    text.trim().toString()
)
