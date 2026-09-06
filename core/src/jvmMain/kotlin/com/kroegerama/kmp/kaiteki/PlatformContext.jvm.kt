package com.kroegerama.kmp.kaiteki

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

@Volatile
private var sharedContext: PlatformContext? = null
private val lock = Any()

public actual abstract class PlatformContext private constructor() {

    /** Identifier of the application, e.g. `com.example.app`; names its data and cache directories. */
    public abstract val applicationId: String

    public companion object {
        /**
         * Sets up the shared [platformContext] with an explicit id instead of the detected one. Call at startup before accessing it.
         *
         * @param applicationId non-blank identifier, usually in reverse-DNS form
         */
        public fun initialize(applicationId: String) {
            require(applicationId.isNotBlank()) { "applicationId must not be blank" }
            sharedContext = create(applicationId)
        }

        private fun create(applicationId: String): PlatformContext = object : PlatformContext() {
            override val applicationId: String = applicationId
        }

        internal fun detectOrCreate(): PlatformContext = sharedContext ?: synchronized(lock) {
            sharedContext ?: create(
                checkNotNull(ApplicationIdDetector.detect(System::getProperty, ::mainThreadEntryClass)) {
                    "Could not detect an application id; call PlatformContext.initialize(applicationId) or set the kaiteki.applicationId system property"
                }
            ).also { sharedContext = it }
        }
    }
}

/**
 * The shared context, created on first access with the detected application id unless [PlatformContext.initialize] was called.
 *
 * @throws IllegalStateException if no application id could be detected
 */
public actual val platformContext: PlatformContext get() = PlatformContext.detectOrCreate()

private fun mainThreadEntryClass(): String? = Thread.getAllStackTraces()
    .entries.firstOrNull { (thread, _) -> thread.name == "main" }
    ?.value?.lastOrNull()?.className

internal object ApplicationIdDetector {

    fun detect(property: (String) -> String?, mainThreadEntryClass: () -> String?): String? =
        property("kaiteki.applicationId").nonBlank()
            ?: property("jpackage.app-path").nonBlank()?.let { File(it).nameWithoutExtension }.nonBlank()
            ?: mainThreadEntryClass().nonBlank()
            ?: property("sun.java.command").nonBlank()?.substringBefore(' ')?.let { command ->
                if (command.endsWith(".jar", ignoreCase = true)) File(command).nameWithoutExtension else command
            }.nonBlank()

    private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

public actual val PlatformContext.dataDirectory: Path
    get() = DesktopDirectories.data(DesktopOs.current, userHome, System::getenv, applicationId)
        .also(FileSystem.SYSTEM::createDirectories)

public actual val PlatformContext.cacheDirectory: Path
    get() = DesktopDirectories.cache(DesktopOs.current, userHome, System::getenv, applicationId)
        .also(FileSystem.SYSTEM::createDirectories)

private val userHome: Path get() = System.getProperty("user.home").toPath()

internal enum class DesktopOs {
    WINDOWS, MACOS, OTHER;

    companion object {
        val current: DesktopOs
            get() {
                val name = System.getProperty("os.name").orEmpty()
                return when {
                    name.startsWith("Windows", ignoreCase = true) -> WINDOWS
                    name.startsWith("Mac", ignoreCase = true) || name.equals("Darwin", ignoreCase = true) -> MACOS
                    else -> OTHER
                }
            }
    }
}

internal object DesktopDirectories {

    fun data(os: DesktopOs, home: Path, env: (String) -> String?, applicationId: String): Path = when (os) {
        DesktopOs.WINDOWS -> (env("LOCALAPPDATA").toAbsolutePathOrNull() ?: (home / "AppData" / "Local")) / applicationId
        DesktopOs.MACOS -> home / "Library" / "Application Support" / applicationId
        DesktopOs.OTHER -> (env("XDG_DATA_HOME").toAbsolutePathOrNull() ?: (home / ".local" / "share")) / applicationId
    }

    fun cache(os: DesktopOs, home: Path, env: (String) -> String?, applicationId: String): Path = when (os) {
        DesktopOs.WINDOWS -> data(os, home, env, applicationId) / "cache"
        DesktopOs.MACOS -> home / "Library" / "Caches" / applicationId
        DesktopOs.OTHER -> (env("XDG_CACHE_HOME").toAbsolutePathOrNull() ?: (home / ".cache")) / applicationId
    }

    private fun String?.toAbsolutePathOrNull(): Path? = this
        ?.takeIf { it.isNotBlank() }
        ?.toPath()
        ?.takeIf { it.isAbsolute }
}
