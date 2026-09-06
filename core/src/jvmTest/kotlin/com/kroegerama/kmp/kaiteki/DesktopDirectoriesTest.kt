package com.kroegerama.kmp.kaiteki

import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopDirectoriesTest {

    private val appId = "com.example.app"

    @Test
    fun windowsUsesLocalAppData() {
        val home = "C:\\Users\\me".toPath()
        val env = mapOf("LOCALAPPDATA" to "D:\\Local")
        assertEquals("D:\\Local\\com.example.app".toPath(), DesktopDirectories.data(DesktopOs.WINDOWS, home, env::get, appId))
        assertEquals("D:\\Local\\com.example.app\\cache".toPath(), DesktopDirectories.cache(DesktopOs.WINDOWS, home, env::get, appId))
    }

    @Test
    fun windowsFallsBackToHome() {
        val home = "C:\\Users\\me".toPath()
        val env = emptyMap<String, String>()
        assertEquals("C:\\Users\\me\\AppData\\Local\\com.example.app".toPath(), DesktopDirectories.data(DesktopOs.WINDOWS, home, env::get, appId))
    }

    @Test
    fun macosUsesLibrary() {
        val home = "/Users/me".toPath()
        val env = emptyMap<String, String>()
        assertEquals("/Users/me/Library/Application Support/com.example.app".toPath(), DesktopDirectories.data(DesktopOs.MACOS, home, env::get, appId))
        assertEquals("/Users/me/Library/Caches/com.example.app".toPath(), DesktopDirectories.cache(DesktopOs.MACOS, home, env::get, appId))
    }

    @Test
    fun linuxUsesXdgVariables() {
        val home = "/home/me".toPath()
        val env = mapOf("XDG_DATA_HOME" to "/data", "XDG_CACHE_HOME" to "/cache")
        assertEquals("/data/com.example.app".toPath(), DesktopDirectories.data(DesktopOs.OTHER, home, env::get, appId))
        assertEquals("/cache/com.example.app".toPath(), DesktopDirectories.cache(DesktopOs.OTHER, home, env::get, appId))
    }

    @Test
    fun linuxFallsBackToHomeForUnsetBlankOrRelativeXdgVariables() {
        val home = "/home/me".toPath()
        listOf(emptyMap(), mapOf("XDG_DATA_HOME" to " ", "XDG_CACHE_HOME" to ""), mapOf("XDG_DATA_HOME" to "rel", "XDG_CACHE_HOME" to "rel/x"))
            .forEach { env ->
                assertEquals("/home/me/.local/share/com.example.app".toPath(), DesktopDirectories.data(DesktopOs.OTHER, home, env::get, appId), "$env")
                assertEquals("/home/me/.cache/com.example.app".toPath(), DesktopDirectories.cache(DesktopOs.OTHER, home, env::get, appId), "$env")
            }
    }

    @Test
    fun detectorPrefersExplicitProperty() {
        val props = mapOf("kaiteki.applicationId" to "com.example.explicit", "jpackage.app-path" to "C:\\Apps\\MyApp\\MyApp.exe")
        assertEquals("com.example.explicit", ApplicationIdDetector.detect(props::get) { "com.example.MainKt" })
    }

    @Test
    fun detectorUsesJpackageLauncherName() {
        listOf(File("Apps", "MyApp.exe"), File(File("MyApp.app", "Contents"), "MyApp")).map { it.absolutePath }.forEach { path ->
            val props = mapOf("jpackage.app-path" to path)
            assertEquals("MyApp", ApplicationIdDetector.detect(props::get) { "com.example.MainKt" }, path)
        }
    }

    @Test
    fun detectorFallsBackToMainClassThenJavaCommand() {
        assertEquals("com.example.MainKt", ApplicationIdDetector.detect({ null }) { "com.example.MainKt" })
        val mainClass = mapOf("sun.java.command" to "com.example.MainKt --flag")
        assertEquals("com.example.MainKt", ApplicationIdDetector.detect(mainClass::get) { null })
        val jar = mapOf("sun.java.command" to "build/libs/my-app.jar --flag")
        assertEquals("my-app", ApplicationIdDetector.detect(jar::get) { null })
        assertEquals(null, ApplicationIdDetector.detect({ " " }) { "" })
    }

    @Test
    fun initializeCarriesApplicationId() {
        val detected = platformContext.applicationId
        println("detected applicationId: $detected")
        assertTrue(detected.isNotBlank())
        PlatformContext.initialize(appId)
        assertEquals(appId, platformContext.applicationId)
        assertFailsWith<IllegalArgumentException> { PlatformContext.initialize(" ") }
    }
}
