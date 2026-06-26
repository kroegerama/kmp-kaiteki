package com.kroegerama.kmp.kaiteki

import android.content.Context
import androidx.startup.Initializer

// https://firebase.blog/posts/2016/12/how-does-firebase-initialize-on-android
// https://funkymuse.dev/posts/create-data-store-kmp/#initialization
private var _applicationContext: Context? = null

public val applicationContext: Context
    get() = checkNotNull(_applicationContext) {
        "applicationContext accessed before ContextProvider initialized"
    }

public data object ContextProviderInitializer

public class ContextProvider : Initializer<ContextProviderInitializer> {
    override fun create(context: Context): ContextProviderInitializer {
        _applicationContext = context.applicationContext
        return ContextProviderInitializer
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
