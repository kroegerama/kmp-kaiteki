import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    explicitApi()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_1
        languageVersion = KotlinVersion.KOTLIN_2_1

        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    coreLibrariesVersion = "2.1.21"

    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjdk-release=11")
            jvmTarget = JvmTarget.JVM_11
            moduleName = "kmp.kaiteki.paging"
        }
    }
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
            moduleName = "kmp.kaiteki.paging"
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.paging.common)
            implementation(libs.arrow)
            implementation(libs.androidx.lifecycle.viewmodel)
        }
    }
}

android {
    namespace = "com.kroegerama.kmp.kaiteki.paging"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
}

mavenPublishing {
    coordinates(
        artifactId = "kaiteki-paging"
    )
}
