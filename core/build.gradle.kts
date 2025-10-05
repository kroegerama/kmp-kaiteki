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

        optIn.add("kotlin.time.ExperimentalTime")
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
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
            moduleName = "kmp.kaiteki.core"
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.lifecycle.common)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.savedstate)
            implementation(libs.arrow)
            implementation(libs.compose.runtime.annotation)
            implementation(libs.multiplatform.locale)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.kroegerama.kmp.kaiteki"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "kaiteki-core",
        version = version.toString()
    )

    pom(pomAction)
}
