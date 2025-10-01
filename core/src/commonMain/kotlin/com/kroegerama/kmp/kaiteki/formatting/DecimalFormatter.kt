package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import com.vanniktech.locale.Locale

@Immutable
public interface DecimalFormatter {
    @Stable
    public fun format(value: Number): String
}

public expect class DefaultDecimalFormatter
@RememberInComposition constructor(
    locale: Locale,
    minimumFractionDigits: Int = 0,
    minimumIntegerDigits: Int = 1,
    maximumFractionDigits: Int = 3,
    maximumIntegerDigits: Int = 40,
    isGroupingUsed: Boolean = true
) : DecimalFormatter {
    override fun format(value: Number): String
}
