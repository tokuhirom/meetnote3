package meetnote3.utils

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.IntVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.value
import platform.posix.SIGKILL
import platform.posix.STDERR_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.WNOHANG
import platform.posix.close
import platform.posix.dup2
import platform.posix.execlp
import platform.posix.exit
import platform.posix.fork
import platform.posix.kill
import platform.posix.perror
import platform.posix.pipe
import platform.posix.read
import platform.posix.usleep
import platform.posix.waitpid
import kotlin.time.Duration
import kotlin.time.TimeSource

class ProcessBuilder(
    private val command: String,
) {
    @OptIn(ExperimentalForeignApi::class)
    fun start(
        captureStdout: Boolean,
        captureStderr: Boolean,
    ): Process {
        println("Running '$command'")
        memScoped {
            val stdoutPipe: CPointer<IntVarOf<Int>>? = if (captureStdout) {
                val pipe = allocArray<IntVar>(2)
                if (pipe(pipe) != 0) {
                    perror("pipe")
                    error("Failed to create stdout pipe")
                }
                pipe
            } else {
                null
            }
            val stderrPipe: CPointer<IntVarOf<Int>>? = if (captureStderr) {
                val pipe = allocArray<IntVar>(2)
                if (pipe(pipe) != 0) {
                    perror("pipe")
                    error("Failed to create stderr pipe")
                }
                pipe
            } else {
                null
            }

            val pid = fork()
            if (pid < 0) {
                perror("fork")
                error("Failed to fork process")
            } else if (pid == 0) {
                // child process
                if (stdoutPipe != null) {
                    close(stdoutPipe[0])
                    dup2(stdoutPipe[1], STDOUT_FILENO)
                    close(stdoutPipe[1])
                }
                if (stderrPipe != null) {
                    close(stderrPipe[0])
                    dup2(stderrPipe[1], STDERR_FILENO)
                    close(stderrPipe[1])
                }
                execlp("/bin/sh", "sh", "-c", command, null)
                perror("execlp")
                exit(1)
            } else {
                // parent process
                if (stdoutPipe != null) {
                    close(stdoutPipe[1])
                }
                if (stderrPipe != null) {
                    close(stderrPipe[1])
                }

                return Process(
                    pid,
                    if (stdoutPipe != null) {
                        FileDescriptor(stdoutPipe[0])
                    } else {
                        null
                    },
                    if (stderrPipe != null) {
                        FileDescriptor(stderrPipe[0])
                    } else {
                        null
                    },
                )
            }
        }
        error("Unreachable")
    }
}

class Process(
    private val pid: Int,
    val stdout: FileDescriptor?,
    val stderr: FileDescriptor?,
) {
    @OptIn(ExperimentalForeignApi::class)
    fun wait(): Int {
        memScoped {
            val status = alloc<IntVar>()
            if (waitpid(pid, status.ptr, 0) == -1) {
                perror("waitpid")
                throw WaitTimeoutException("Failed to wait for the process")
            }
            return wifexited(status.value)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun waitUntil(duration: Duration): Int {
        memScoped {
            val startTime = TimeSource.Monotonic.markNow()
            val status = alloc<IntVar>()
            while (true) {
                val waitPidResult = waitpid(pid, status.ptr, WNOHANG)
                if (waitPidResult == -1) {
                    perror("waitpid")
                    throw WaitTimeoutException("Failed to wait for the process")
                } else if (waitPidResult == 0) {
                    usleep(100u * 1000u)
                    if (startTime.elapsedNow() > duration) {
                        println("Timeout! Sending SIGKILL to the process")
                        kill(pid, SIGKILL)
                    }
                    continue
                } else {
                    return wifexited(status.value)
                }
            }
        }
        error("Unreachable")
    }

    private fun wifexited(value: Int): Int = (value and 0xff00) shr 8
}

class FileDescriptor(
    private val fd: Int,
) {
    @OptIn(ExperimentalForeignApi::class)
    fun slurpString(): String {
        memScoped {
            val buffer = ByteArray(1024)
            val output = StringBuilder()
            while (true) {
                val bytesRead = read(fd, buffer.refTo(0), buffer.size.toULong())
                if (bytesRead <= 0) break
                val got = buffer.decodeToString(0, bytesRead.toInt())
                output.append(got)
            }
            close(fd)
            return output.toString()
        }
    }

    fun close() {
        close(fd)
    }
}

class WaitTimeoutException(
    message: String,
) : Exception(message)
