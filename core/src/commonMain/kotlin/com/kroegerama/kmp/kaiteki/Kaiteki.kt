package com.kroegerama.kmp.kaiteki

/**
 * Marks a declaration as internal to Kaiteki. It is not part of the public contract and may change
 * or be removed without notice; opting in is required to use it.
 */
@RequiresOptIn(
    message = "This is an internal Kaiteki API. It may change without notice.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
public annotation class InternalKaitekiApi

/**
 * Marks a Kaiteki API as experimental. Its signature or behavior may change in a future release;
 * opting in acknowledges this.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class ExperimentalKaitekiApi
