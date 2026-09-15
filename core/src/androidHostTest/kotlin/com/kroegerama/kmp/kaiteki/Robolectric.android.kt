package com.kroegerama.kmp.kaiteki

import android.os.Build
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
actual abstract class RobolectricTest {
    private companion object {
        init {
            println("Robolectric SDK ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})")
        }
    }
}
