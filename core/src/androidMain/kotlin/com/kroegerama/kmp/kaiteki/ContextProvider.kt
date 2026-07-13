package com.kroegerama.kmp.kaiteki

import android.content.Context
import androidx.startup.Initializer

// https://firebase.blog/posts/2016/12/how-does-firebase-initialize-on-android
// https://funkymuse.dev/posts/create-data-store-kmp/#initialization
private var _applicationContext: Context? = null

/**
 * The application [Context], captured by [ContextProvider] at startup.
 *
 * @throws IllegalStateException if accessed before [ContextProvider] has run.
 */
public val applicationContext: Context
    get() = checkNotNull(_applicationContext) {
        "applicationContext accessed before ContextProvider initialized"
    }

/** Marker result produced by [ContextProvider]. */
public data object ContextProviderInitializer

/**
 * App Startup [Initializer] that captures the application context so it is available through
 * [applicationContext] and [platformContext]. Registered automatically via the library manifest.
 */
public class ContextProvider : Initializer<ContextProviderInitializer> {
    override fun create(context: Context): ContextProviderInitializer {
        _applicationContext = context.applicationContext
        return ContextProviderInitializer
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
