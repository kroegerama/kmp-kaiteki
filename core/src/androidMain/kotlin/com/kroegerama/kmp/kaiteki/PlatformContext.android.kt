package com.kroegerama.kmp.kaiteki

import okio.Path
import okio.Path.Companion.toOkioPath

public actual typealias PlatformContext = android.content.Context

public actual val platformContext: PlatformContext get() = applicationContext

public actual val PlatformContext.dataDirectory: Path get() = filesDir.toOkioPath()

public actual val PlatformContext.cacheDirectory: Path get() = cacheDir.toOkioPath()
