package com.kroegerama.kmp.kaiteki

import android.content.Context
import androidx.startup.Initializer

// https://firebase.blog/posts/2016/12/how-does-firebase-initialize-on-android
// https://funkymuse.dev/posts/create-data-store-kmp/#initialization
public lateinit var applicationContext: Context
    private set

public data object ContextProviderInitializer

public class ContextProvider : Initializer<ContextProviderInitializer> {
    override fun create(context: Context): ContextProviderInitializer {
        applicationContext = context.applicationContext
        return ContextProviderInitializer
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
