import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.versions)
}

val projectVersion = providers.gradleProperty("kaiteki.version").get()
logger.lifecycle("kaiteki.version $projectVersion")

val projectGroup = "com.kroegerama.kmp.kaiteki"
val projectDescription = "A set of helper classes for modern Kotlin multiplatform projects."
val projectUrl = "https://github.com/kroegerama/kmp-kaiteki"

val pomAction = Action<MavenPom> {
    name = "KMP Kaiteki"
    description = projectDescription
    inceptionYear = "2025"
    url = projectUrl

    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }
    developers {
        developer {
            id.set("kroegerama")
            name.set("Chris")
            email.set("1519044+kroegerama@users.noreply.github.com")
        }
    }
    scm {
        url.set(projectUrl)
        connection.set("scm:git:https://github.com/kroegerama/kmp-kaiteki")
        developerConnection.set("scm:git:https://www.github.com/kroegerama")
    }
}

allprojects {
    version = projectVersion
    group = projectGroup
    description = projectDescription

    plugins.withId("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()
            coordinates(
                groupId = group.toString(),
                version = version.toString()
            )
            pom(pomAction)
        }
    }
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    gradleReleaseChannel = "current"
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

private val nonStableQualifiers = listOf("alpha", "beta", "rc")

private fun isNonStable(version: String): Boolean = nonStableQualifiers.any { qualifier ->
    qualifier in version.lowercase()
}
