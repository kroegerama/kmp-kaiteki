package com.kroegerama.kmp.kaiteki

import android.os.Build

public actual object Platform {
    public actual val name: String = "Android ${Build.VERSION.SDK_INT}"
}
