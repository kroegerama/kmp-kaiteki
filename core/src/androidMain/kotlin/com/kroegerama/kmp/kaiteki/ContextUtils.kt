package com.kroegerama.kmp.kaiteki

import android.content.Context
import android.content.pm.PackageManager
import android.util.TypedValue
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import java.nio.charset.Charset

/** Returns `true` if [permission] is currently granted to the app. */
public fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** Resolves the resource id referenced by theme attribute [attrRes], e.g. a `?attr/...` reference. */
@AnyRes
public fun Context.resolveResourceIdAttribute(@AttrRes attrRes: Int): Int = TypedValue().apply {
    theme.resolveAttribute(attrRes, this, true)
}.resourceId

/** Reads the raw resource [rawRes] fully into a string using [charset]. */
public fun Context.getRawResAsString(
    @RawRes rawRes: Int,
    charset: Charset = Charsets.UTF_8
): String = resources.openRawResource(rawRes).bufferedReader(charset).use { reader ->
    reader.readText()
}
