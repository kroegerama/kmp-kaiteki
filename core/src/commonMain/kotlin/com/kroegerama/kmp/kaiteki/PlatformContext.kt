package com.kroegerama.kmp.kaiteki

import okio.Path

/**
 * Common handle to the platform-specific application context.
 *
 * On Android this is a `typealias` for `android.content.Context`; on Apple platforms it is an opaque
 * placeholder type; on the JVM it carries an application id, detected from the launch environment or set
 * via `PlatformContext.initialize`. Obtain the shared instance via [platformContext].
 */
public expect abstract class PlatformContext

/**
 * The shared [PlatformContext] for the current platform.
 *
 * On Android this is the application context and requires the `ContextProvider` startup initializer
 * to have run. On the JVM the application id is taken from the `kaiteki.applicationId` system property, the
 * jpackage launcher name, the main class, or the jar name, unless `PlatformContext.initialize(applicationId)` was called.
 */
public expect val platformContext: PlatformContext

/**
 * Directory for persistent, app-private files; it exists when this returns.
 * Android uses `filesDir`, Apple `Library/Application Support`, the JVM the OS-specific per-user data
 * location (`%LOCALAPPDATA%`, `~/Library/Application Support`, `$XDG_DATA_HOME`) plus the application id.
 *
 * @throws okio.IOException if the directory is missing and cannot be created
 */
public expect val PlatformContext.dataDirectory: Path

/**
 * Directory for cache files the system may reclaim; it exists when this returns.
 * Android uses `cacheDir`, Apple `Library/Caches`, the JVM the OS-specific per-user cache location
 * (`%LOCALAPPDATA%\<id>\cache`, `~/Library/Caches`, `$XDG_CACHE_HOME`) plus the application id.
 *
 * @throws okio.IOException if the directory is missing and cannot be created
 */
public expect val PlatformContext.cacheDirectory: Path
