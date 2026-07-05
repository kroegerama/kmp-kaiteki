# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ContextProvider is instantiated reflectively by androidx.startup's
# InitializationProvider from the manifest <meta-data> entry.
-keep class com.kroegerama.kmp.kaiteki.ContextProvider
