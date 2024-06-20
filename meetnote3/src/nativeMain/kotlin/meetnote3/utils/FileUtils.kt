package meetnote3.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.F_OK
import platform.posix.access
import platform.posix.perror
import platform.posix.stat

fun fileExists(fileName: String): Boolean = access(fileName, F_OK) == 0

@OptIn(ExperimentalForeignApi::class)
fun getFileSize(fileName: String): Long {
    memScoped {
        val fileStat = alloc<stat>()
        if (stat(fileName, fileStat.ptr) != 0) {
            perror("stat")
            throw RuntimeException("Failed to get file size")
        }
        return fileStat.st_size
    }
}
