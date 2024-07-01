package meetnote3.utils

import meetnote3.info
import platform.posix.SIGKILL
import platform.posix.STDERR_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.WNOHANG
import platform.posix.close
import platform.posix.dup2
import platform.posix.execvp
import platform.posix.exit
import platform.posix.fork
import platform.posix.kill
import platform.posix.perror
import platform.posix.pipe
import platform.posix.read
import platform.posix.waitpid

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.IntVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import kotlinx.coroutines.delay

class ProcessBuilder(
    private vararg val command: String,
) {
    @OptIn(ExperimentalForeignApi::class)
    fun start(
        captureStdout: Boolean,
        captureStderr: Boolean,
    ): Process {
        println("Running '${command.joinToString(" ")}'")
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

                val args = (command.toList() + listOf(null)).map { it?.cstr?.ptr }.toCValues()
                execvp(command[0], args)
                perror("execvp")
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
                    command,
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
    private val command: Array<out String>,
    private val pid: Int,
    val stdout: FileDescriptor?,
    val stderr: FileDescriptor?,
) {
    @OptIn(ExperimentalForeignApi::class)
    fun wait(): ProcessExitStatus {
        memScoped {
            val status = alloc<IntVar>()
            if (waitpid(pid, status.ptr, 0) == -1) {
                perror("waitpid")
                throw WaitTimeoutException("Failed to wait for the process")
            }
            return ProcessExitStatus(status.value)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    suspend fun waitUntil(
        duration: Duration,
        sleepInterval: Duration = Duration.parse("1s"),
    ): ProcessExitStatus {
        memScoped {
            val startTime = TimeSource.Monotonic.markNow()
            val status = alloc<IntVar>()
            var timeout = false
            while (true) {
                val waitPidResult = waitpid(pid, status.ptr, WNOHANG)
                if (waitPidResult == -1) {
                    perror("waitpid")
                    error("Failed to wait for the process")
                } else if (waitPidResult == 0) {
                    delay(sleepInterval)
                    if (startTime.elapsedNow() > duration) {
                        info("Timeout! Sending SIGKILL to the process(${command.toList()}): $duration exceeded.")
                        kill(pid, SIGKILL)
                        timeout = true
                    }
                    continue
                } else {
                    val processExitStatus = ProcessExitStatus(status.value)
                    if (timeout && processExitStatus.signalled() && processExitStatus.termsig() == SIGKILL) {
                        throw ProcessTimeoutException()
                    }
                    return processExitStatus
                }
            }
        }
        error("Unreachable")
    }

    class ProcessTimeoutException : Exception()
}

data class ProcessExitStatus(
    val value: Int,
) {
    fun exited(): Boolean = wifexited()

    fun exitstatus(): Int {
        if (wifexited()) {
            return wexitstatus()
        } else {
            error("Process did not exit normally: status = $value")
        }
    }

    fun signalled(): Boolean = wifsignaled()

    fun termsig(): Int {
        if (wifsignaled()) {
            return wtermsig()
        } else {
            error("Process was not terminated by signal: status = $value")
        }
    }

    private fun wifexited(): Boolean = (value and 0xff) == 0

    private fun wexitstatus(): Int = (value shr 8) and 0xff

    private fun wifsignaled(): Boolean = (value and 0xff) != 0 && (value and 0x7f) != 0x7f

    private fun wtermsig(): Int = value and 0x7f

    override fun toString(): String =
        "ProcessExitStatus(value=$value, exited=${exited()}, exitstatus=${exitstatus()}," +
            " signalled=${signalled()}, termsig=${termsig()}"
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
