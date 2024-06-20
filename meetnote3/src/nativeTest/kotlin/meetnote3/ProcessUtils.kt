package meetnote3

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
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

const val BINARY_PATH = "./build/bin/native/debugExecutable/capjoy.kexe"

class ProcessBuilder(
    val command: String,
) {
    @OptIn(ExperimentalForeignApi::class)
    fun start(enableDebugging: Boolean = false): Process {
        println("Running '$command'")
        memScoped {
            val stdoutPipe = allocArray<IntVar>(2)
            val stderrPipe = allocArray<IntVar>(2)

            if (pipe(stdoutPipe) != 0) {
                perror("pipe")
                error("Failed to create stdout pipe")
            }
            if (pipe(stderrPipe) != 0) {
                perror("pipe")
                error("Failed to create stderr pipe")
            }

            val pid = fork()
            if (pid < 0) {
                perror("fork")
                error("Failed to fork process")
            } else if (pid == 0) {
                // child process
                close(stdoutPipe[0])
                close(stderrPipe[0])
                dup2(stdoutPipe[1], STDOUT_FILENO)
                dup2(stderrPipe[1], STDERR_FILENO)
                close(stdoutPipe[1])
                close(stderrPipe[1])
                execlp("/bin/sh", "sh", "-c", command, null)
                perror("execlp")
                exit(1)
            } else {
                // parent process
                close(stdoutPipe[1])
                close(stderrPipe[1])

                return Process(pid, FileDescriptor(stdoutPipe[0]), FileDescriptor(stderrPipe[0]))
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

fun runCommand(command: String): Pair<Int, String> {
    val builder = ProcessBuilder(command)
    val process = builder.start()
    val stdout = process.stdout!!.slurpString()
    val stderr = process.stderr!!.slurpString()
    val exitCode = process.wait()
    return exitCode to "stdout: $stdout\nstderr: $stderr"
}
