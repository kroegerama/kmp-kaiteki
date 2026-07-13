package com.kroegerama.kmp.kaiteki

/** Provides information about the platform the code is currently running on. */
public expect object Platform {
    /**
     * Human-readable platform name including version, e.g. `"Android 34"`, `"iOS 17.0"` or
     * `"Java 21"`.
     */
    public val name: String
}
