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
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2

        optIn.addAll(
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.time.ExperimentalTime"
        )

        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    coreLibrariesVersion = "2.2.21"

    android {
        namespace = "com.kroegerama.kmp.kaiteki.camera"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt())
        }
        minSdk {
            version = release(libs.versions.android.minSdk.get().toInt())
        }
        enableCoreLibraryDesugaring = true

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
            moduleName = "kmp.kaiteki.camera"
        }

        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.file("consumer-proguard-rules.pro")
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)

            implementation(projects.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.compose)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.lifecycle)

            implementation(libs.mlkit.barcode.scanning)
            implementation(libs.mlkit.text.recognition)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    coreLibraryDesugaring(libs.desugar)
}

mavenPublishing {
    coordinates(
        artifactId = "kaiteki-camera"
    )
}
