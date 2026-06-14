package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import java.text.DecimalFormat

public actual class DefaultDecimalFormatter @RememberInComposition actual constructor(
    locale: PlatformLocale,
    minimumFractionDigits: Int,
    minimumIntegerDigits: Int,
    maximumFractionDigits: Int,
    maximumIntegerDigits: Int,
    isGroupingUsed: Boolean
) : DecimalFormatter {
    private val decimalFormat = DecimalFormat.getInstance(locale).apply {
        this.minimumFractionDigits = minimumFractionDigits
        this.minimumIntegerDigits = minimumIntegerDigits
        this.maximumFractionDigits = maximumFractionDigits
        this.maximumIntegerDigits = maximumIntegerDigits
        this.isGroupingUsed = isGroupingUsed
    } as DecimalFormat

    actual override fun format(value: Number): String = decimalFormat.format(value)

    actual override val decimalSeparator: Char = decimalFormat.decimalFormatSymbols.decimalSeparator

    actual override val groupingSeparator: Char = decimalFormat.decimalFormatSymbols.groupingSeparator

}
