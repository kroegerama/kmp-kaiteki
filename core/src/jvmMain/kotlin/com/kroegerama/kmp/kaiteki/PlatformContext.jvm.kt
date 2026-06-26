package com.kroegerama.kmp.kaiteki

public actual abstract class PlatformContext private constructor() {
    public companion object {
        public val INSTANCE: PlatformContext = object : PlatformContext() {}
    }
}

public actual val platformContext: PlatformContext get() = PlatformContext.INSTANCE
