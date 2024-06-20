package meetnote3.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.posix.EEXIST
import platform.posix.S_IRWXG
import platform.posix.S_IRWXO
import platform.posix.S_IRWXU
import platform.posix.getenv
import platform.posix.mkdir

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

fun generateTimestamp(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val dateTimeString = now.toString().replace(":", "-")
    return dateTimeString
}

data class DocumentDirectory(
    val dir: String,
) {
    fun micFilePath() = "$dir/mic.m4a"

    fun screenFilePath() = "$dir/screen.m4a"

    fun mixedFilePath() = "$dir/mixed.m4a"

    companion object {
        fun create(): DocumentDirectory {
            val home = getHomeDirectory()
            val dateTimeString = generateTimestamp()
            val directory = "$home/Documents/MeetNote3/$dateTimeString"
            mkdirP(directory)
            return DocumentDirectory(directory)
        }
    }
}
