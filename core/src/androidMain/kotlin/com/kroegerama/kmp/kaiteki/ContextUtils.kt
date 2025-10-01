package com.kroegerama.kmp.kaiteki

import android.content.Context
import android.content.pm.PackageManager
import android.util.TypedValue
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import java.nio.charset.Charset

public fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@AnyRes
public fun Context.resolveResourceIdAttribute(@AttrRes attrRes: Int): Int = TypedValue().apply {
    theme.resolveAttribute(attrRes, this, true)
}.resourceId

public fun Context.getRawResAsString(
    @RawRes rawRes: Int,
    charset: Charset = Charsets.UTF_8
): String = resources.openRawResource(rawRes).bufferedReader(charset).use { reader ->
    reader.readText()
}
