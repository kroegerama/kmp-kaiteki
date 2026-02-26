import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    explicitApi()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_1
        languageVersion = KotlinVersion.KOTLIN_2_1

        optIn.addAll(
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.time.ExperimentalTime",
            "kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    coreLibrariesVersion = "2.1.21"

    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjdk-release=11")
            jvmTarget = JvmTarget.JVM_11
            moduleName = "kmp.kaiteki.core"
        }
    }

    androidLibrary {
        namespace = "com.kroegerama.kmp.kaiteki"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        enableCoreLibraryDesugaring = true

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
            moduleName = "kmp.kaiteki.core"
        }

        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.file("consumer-proguard-rules.pro")
        }

        withHostTest { }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.jb.androidx.lifecycle.common)
            implementation(libs.jb.androidx.lifecycle.runtime)
            implementation(libs.jb.androidx.lifecycle.viewmodel.savedstate)
            implementation(libs.arrow)
            implementation(libs.compose.runtime.annotation)
            implementation(libs.multiplatform.locale)
        }
        androidMain.dependencies {
            implementation(libs.androidx.startup)
        }
        jvmMain.dependencies {
            implementation(libs.icu4j)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        val androidHostTest by getting
        androidHostTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
}

mavenPublishing {
    coordinates(
        artifactId = "kaiteki-core"
    )
}
