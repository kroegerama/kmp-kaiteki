package com.kroegerama.kmp.kaiteki

/**
 * This can be used for dependency injection e.g. with Metro
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
    public fun init(isDebug: Boolean)
}
