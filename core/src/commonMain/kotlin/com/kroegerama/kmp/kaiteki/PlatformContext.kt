package com.kroegerama.kmp.kaiteki

/**
 * Common handle to the platform-specific application context.
 *
 * On Android this is a `typealias` for `android.content.Context`; on other platforms it is an opaque
 * placeholder type. Obtain the shared instance via [platformContext].
 */
public expect abstract class PlatformContext

/**
 * The shared [PlatformContext] for the current platform.
 *
 * On Android this is the application context and requires the `ContextProvider` startup initializer
 * to have run.
 */
public expect val platformContext: PlatformContext
