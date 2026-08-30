# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Members exposed through addJavascriptInterface are only ever called from JavaScript,
# so nothing in the bytecode keeps them reachable.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
