package meetnote3.utils

import platform.posix.EEXIST
import platform.posix.F_OK
import platform.posix.S_IRWXG
import platform.posix.S_IRWXO
import platform.posix.S_IRWXU
import platform.posix.access
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getenv
import platform.posix.mkdir
import platform.posix.strerror

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString

fun fileExists(fileName: String): Boolean = access(fileName, F_OK) == 0

@OptIn(ExperimentalForeignApi::class)
fun spurt(
    filePath: String,
    content: String,
) {
    val file = fopen(filePath, "w") ?: error("Failed to open file: $filePath")
    if (fwrite(content.cstr, 1u, content.length.toULong(), file) != content.length.toULong()) {
        fclose(file)
        error("Failed to write to file: $filePath: ${strerror(errno)}")
    }

    fclose(file)
}

@OptIn(ExperimentalForeignApi::class)
fun mkdirP(path: String) {
    val parts = path.split('/')
    var currentPath = ""

    for (part in parts) {
        if (part.isEmpty()) continue // skip empty parts, like the one before the leading '/'

        currentPath += "/$part"

        val result = mkdir(currentPath, (S_IRWXU or S_IRWXG or S_IRWXO).convert())
        if (result != 0) {
            val error = platform.posix.errno
            if (error == EEXIST) {
                // If the directory already exists, continue to the next part
                continue
            } else {
                throw RuntimeException(
                    "Error creating directory $currentPath: ${
                        platform.posix.strerror(error)?.toKString()
                    }",
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun getHomeDirectory(): String = getenv("HOME")?.toKString() ?: throw IllegalStateException("Home directory not found")
