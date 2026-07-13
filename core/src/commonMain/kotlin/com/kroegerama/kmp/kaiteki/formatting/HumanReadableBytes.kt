package com.kroegerama.kmp.kaiteki.formatting

import kotlin.math.ln
import kotlin.math.pow

/**
 * Formats this byte count as a short human-readable string, e.g. `1536.asHumanReadableBytes()` →
 * `"1.5 KiB"`.
 *
 * @param si when `true`, uses decimal SI units (powers of 1000: `kB`, `MB`, …); when `false`,
 *   binary units (powers of 1024: `KiB`, `MiB`, …).
 */
public fun Long.asHumanReadableBytes(si: Boolean = false): String {
    val unit = if (si) 1000f else 1024f
    if (this < unit) return "$this B"
    val exp = (ln(toDouble()) / ln(unit.toDouble())).toInt()

    //Long.MAX_VALUE -> 9,2 EB / 8,0 EiB
    val chars = if (si) "kMGTPEZY" else "KMGTPEZY"
    val pre = chars[exp - 1] + if (si) "" else "i"
    val value = this / unit.pow(exp.toFloat())
    return formatHumanReadableBytes(value, pre)
}

internal expect fun formatHumanReadableBytes(value: Float, pre: String): String
