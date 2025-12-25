package com.kroegerama.kmp.kaiteki

@RequiresOptIn(
    message = "This is an internal Kaiteki API. It may change without notice.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
public annotation class InternalKaitekiApi
