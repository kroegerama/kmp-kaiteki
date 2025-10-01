package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.annotation.RememberInComposition
import com.vanniktech.locale.Locale
import com.vanniktech.locale.toJavaLocale
import java.text.DecimalFormat

public actual class DefaultDecimalFormatter @RememberInComposition actual constructor(
    locale: Locale,
    minimumFractionDigits: Int,
    minimumIntegerDigits: Int,
    maximumFractionDigits: Int,
    maximumIntegerDigits: Int,
    isGroupingUsed: Boolean
) : DecimalFormatter {
    private val decimalFormat = DecimalFormat.getInstance(locale.toJavaLocale()).apply {
        this.minimumFractionDigits = minimumFractionDigits
        this.minimumIntegerDigits = minimumIntegerDigits
        this.maximumFractionDigits = maximumFractionDigits
        this.maximumIntegerDigits = maximumIntegerDigits
        this.isGroupingUsed = isGroupingUsed
    }

    actual override fun format(value: Number): String = decimalFormat.format(value)
}
