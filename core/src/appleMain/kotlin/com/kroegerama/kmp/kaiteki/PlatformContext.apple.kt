package com.kroegerama.kmp.kaiteki

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

public actual abstract class PlatformContext private constructor() {
    public companion object {
        public val INSTANCE: PlatformContext = object : PlatformContext() {}
    }
}

public actual val platformContext: PlatformContext get() = PlatformContext.INSTANCE

public actual val PlatformContext.dataDirectory: Path get() = userDirectory(NSApplicationSupportDirectory)

public actual val PlatformContext.cacheDirectory: Path get() = userDirectory(NSCachesDirectory)

private fun userDirectory(directory: NSSearchPathDirectory): Path {
    val path = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true).first() as String
    return path.toPath().also(FileSystem.SYSTEM::createDirectories)
}
