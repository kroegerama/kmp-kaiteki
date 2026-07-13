package com.kroegerama.kmp.kaiteki

/**
 * Marker interface for startup logic that should run once when the application launches.
 *
 * Collect all implementations through a dependency-injection framework (e.g. Metro) and invoke
 * [init] on each at startup.
 *
 * ```kotlin
 * @ContributesIntoSet(AppScope::class)
 * @SingleIn(AppScope::class)
 * @Inject
 * class MyInitializer: Initializer {
 *
 * }
 * ```
 *
 * ```kotlin
 * @DependencyGraph(AppScope::class)
 * interface AppGraph {
 *     val initializers: Set<Initializer>
 * }
 * ```
 *
 * ```kotlin
 * fun init(isDebug: Boolean) {
 *   appGraph.initializers.forEach { it.init(isDebug) }
 * }
 * ```
 */
public interface Initializer {
    /** Runs the initialization logic. [isDebug] signals a debug build so behavior can be adjusted. */
    public fun init(isDebug: Boolean)
}
