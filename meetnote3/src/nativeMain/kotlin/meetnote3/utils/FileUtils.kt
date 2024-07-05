package meetnote3.utils

import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
fun getHomeDirectory(): Path = getenv("HOME")?.toKString()?.toPath() ?: throw IllegalStateException("Home directory not found")
