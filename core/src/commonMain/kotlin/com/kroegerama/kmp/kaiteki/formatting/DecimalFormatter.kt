package com.kroegerama.kmp.kaiteki.formatting

import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import com.kroegerama.kmp.kaiteki.locale.PlatformLocale
import com.kroegerama.kmp.kaiteki.locale.currentPlatformLocale

@Stable
public interface DecimalFormatter {
    @Stable
    public fun format(value: Number): String

    @Stable
    public val decimalSeparator: Char

    @Stable
    public val groupingSeparator: Char
}

public expect class DefaultDecimalFormatter
@RememberInComposition constructor(
    locale: PlatformLocale = currentPlatformLocale(),
    minimumFractionDigits: Int = 0,
    minimumIntegerDigits: Int = 1,
    maximumFractionDigits: Int = 3,
    maximumIntegerDigits: Int = 40,
    isGroupingUsed: Boolean = true
) : DecimalFormatter {
    override fun format(value: Number): String
    override val decimalSeparator: Char
    override val groupingSeparator: Char
}
