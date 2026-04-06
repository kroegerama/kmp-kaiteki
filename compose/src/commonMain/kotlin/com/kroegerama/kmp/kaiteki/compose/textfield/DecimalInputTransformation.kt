package com.kroegerama.kmp.kaiteki.compose.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kroegerama.kmp.kaiteki.compose.formatting.rememberDecimalFormatter
import com.kroegerama.kmp.kaiteki.formatting.DecimalFormatter

@Stable
public class DecimalInputTransformation @RememberInComposition constructor(
    decimalFormatter: DecimalFormatter,
    private val maxDecimalPlaces: Int = 3
) : InputTransformation {

    private val decimalSeparator = decimalFormatter.decimalSeparator
    private val decimalSeparatorConsecutive = "$decimalSeparator$decimalSeparator"

    override val keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Decimal
    )

    override fun TextFieldBuffer.transformInput() {
        if (asCharSequence().any { !it.isDigit() && it != decimalSeparator }) {
            revertAllChanges()
            return
        }
        if (decimalSeparatorConsecutive in asCharSequence()) {
            revertAllChanges()
            return
        }

        val parts = asCharSequence().split(decimalSeparator)
        val intPart = parts.firstOrNull() ?: ""
        val decimalPart = parts.getOrNull(1)
        if (parts.size > 2) {
            // should never happen
            revertAllChanges()
            return
        }

        val filteredIntPart = filterIntPart(intPart, hasDecimalPart = decimalPart != null)
        filterDecimalPart(decimalPart, startIndex = filteredIntPart.length + 1)
    }

    private fun TextFieldBuffer.filterIntPart(
        intPart: String,
        hasDecimalPart: Boolean,
    ): String {
        var mutableIntPart = intPart

        if (mutableIntPart.startsWith('0')) {
            // Allow single leading zero. Replace leading zero if followed by another digit.
            val indexOfFirstNonZero = mutableIntPart.indexOfFirst { it != '0' }
            val newIntPart = if (indexOfFirstNonZero > 0) {
                mutableIntPart.substring(indexOfFirstNonZero)
            } else {
                "0"
            }
            replace(0, mutableIntPart.length, newIntPart)
            mutableIntPart = newIntPart
        }

        if (mutableIntPart.isEmpty() && hasDecimalPart) {
            // Prefill 0 when only decimal part is entered
            val newIntPart = "0"
            replace(0, mutableIntPart.length, newIntPart)
            mutableIntPart = newIntPart
        }

        return mutableIntPart
    }

    private fun TextFieldBuffer.filterDecimalPart(
        decimalPart: String?,
        startIndex: Int,
    ) {
        if (decimalPart != null && decimalPart.length > maxDecimalPlaces) {
            delete(startIndex + maxDecimalPlaces, length)
        }
    }
}

@Composable
@Stable
public fun InputTransformation.decimalInput(
    decimalFormatter: DecimalFormatter = rememberDecimalFormatter(),
    maxDecimalPlaces: Int = 3
): InputTransformation = this.then(
    DecimalInputTransformation(
        decimalFormatter = decimalFormatter,
        maxDecimalPlaces = maxDecimalPlaces
    )
)

@Preview
@Composable
private fun DecimalInputTransformationPreview() {
    MaterialTheme {
        Scaffold { innerPadding ->
            OutlinedTextField(
                state = rememberTextFieldState(),
                inputTransformation = InputTransformation.decimalInput(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        }
    }
}
