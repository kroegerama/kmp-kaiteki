import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
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
            "kotlin.time.ExperimentalTime"
        )

        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    coreLibrariesVersion = "2.1.21"

    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjdk-release=11")
            jvmTarget = JvmTarget.JVM_11
            moduleName = "kmp.kaiteki.compose"
        }
    }

    android {
        namespace = "com.kroegerama.kmp.kaiteki.compose"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        enableCoreLibraryDesugaring = true

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
            moduleName = "kmp.kaiteki.compose"
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.jb.androidx.lifecycle.viewmodel)
            implementation(libs.arrow)
            implementation(libs.multiplatform.locale)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.preview)

            api(project((":core")))
        }
        androidMain.dependencies {
            implementation(libs.androidx.browser)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core)
            implementation(libs.androidx.navigation.runtime)
            implementation(libs.hilt.lifecycle.viewmodel.compose)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    coreLibraryDesugaring(libs.desugar)
}

mavenPublishing {
    coordinates(
        artifactId = "kaiteki-compose"
    )
}
