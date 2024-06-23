package meetnote3.utils

import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

// https://wiki.archlinux.org/title/XDG_Base_Directory
@OptIn(ExperimentalForeignApi::class)
object XdgDirectories {
    private const val XDG_CONFIG_HOME = "XDG_CONFIG_HOME"
    private const val XDG_DATA_HOME = "XDG_DATA_HOME"
    private const val HOME = "HOME"

    fun getConfigDir(): Path {
        val configDir = getenv(XDG_CONFIG_HOME)?.toKString() ?: "${getHomeDir()}/.config"
        return configDir.toPath()
    }

    fun getShareDir(): Path {
        val shareDir = getenv(XDG_DATA_HOME)?.toKString() ?: "${getHomeDir()}/.local/share"
        return shareDir.toPath()
    }

    private fun getHomeDir(): String = getenv(HOME)?.toKString() ?: throw IllegalStateException("HOME environment variable is not set")
}

class XdgAppDirectories(
    val appName: String,
) {
    fun getConfigDir(): Path {
        val configDir = XdgDirectories.getConfigDir()
        return configDir / appName
    }

    fun getShareDir(): Path {
        val shareDir = XdgDirectories.getShareDir()
        return shareDir / appName
    }
}
