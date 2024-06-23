package meetnote3.utils

import platform.posix.dup2
import platform.posix.fileno
import platform.posix.fopen
import platform.posix.setbuf
import platform.posix.stderr
import platform.posix.stdout

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun redirectOutput(stdoutFilePath: String) {
    println("Redirecting output to $stdoutFilePath") // Debug log

    // Open the file for writing
    val file = fopen(stdoutFilePath, "w") ?: throw Error("Failed to open $stdoutFilePath")
    println("File opened successfully") // Debug log

    // Get the file descriptor of the opened file
    val fileFd = fileno(file)
    if (fileFd == -1) {
        throw Error("Failed to get file descriptor for $stdoutFilePath")
    }
    println("File descriptor obtained: $fileFd") // Debug log

    // Duplicate the stdout file descriptor to the file
    if (dup2(fileFd, fileno(stdout)) == -1) {
        throw Error("Failed to duplicate stdout file descriptor to $stdoutFilePath")
    }
    println("stdout duplicated") // Debug log

    // Duplicate the stderr file descriptor to the file
    if (dup2(fileFd, fileno(stderr)) == -1) {
        throw Error("Failed to duplicate stderr file descriptor to $stdoutFilePath")
    }
    println("stderr duplicated") // Debug log

    setbuf(stdout, null) // disable buffering
    setbuf(stderr, null) // disable buffering
    println("Buffering disabled") // Debug log
}
