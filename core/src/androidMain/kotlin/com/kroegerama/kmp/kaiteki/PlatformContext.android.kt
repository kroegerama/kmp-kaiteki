package com.kroegerama.kmp.kaiteki

public actual typealias PlatformContext = android.content.Context

public actual val platformContext: PlatformContext get() = applicationContext
