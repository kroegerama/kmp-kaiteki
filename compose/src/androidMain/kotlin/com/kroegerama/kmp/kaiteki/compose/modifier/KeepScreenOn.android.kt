package com.kroegerama.kmp.kaiteki.compose.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn as platformKeepScreenOn

public actual fun Modifier.keepScreenOn(): Modifier = platformKeepScreenOn()
