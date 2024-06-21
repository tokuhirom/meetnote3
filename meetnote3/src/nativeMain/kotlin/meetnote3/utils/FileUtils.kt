package meetnote3.utils

import okio.Path
import okio.Path.Companion.toPath
import platform.posix.F_OK
import platform.posix.access
import platform.posix.getenv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

fun fileExists(fileName: String): Boolean = access(fileName, F_OK) == 0

@OptIn(ExperimentalForeignApi::class)
fun getHomeDirectory(): Path = getenv("HOME")?.toKString()?.toPath() ?: throw IllegalStateException("Home directory not found")
