import meetnote3.initLogger
import meetnote3.model.generateTimestamp
import meetnote3.service.CaptureMixService
import meetnote3.service.RecordingService
import meetnote3.service.RecoveringService
import meetnote3.service.SummarizeService
import meetnote3.service.WhisperTranscriptService
import meetnote3.service.WindowMonitoringService
import meetnote3.utils.ProcessBuilder
import meetnote3.utils.XdgAppDirectories
import okio.FileSystem
import platform.CoreFoundation.CFRunLoopRun
import platform.posix.dup2
import platform.posix.fileno
import platform.posix.fopen
import platform.posix.setbuf
import platform.posix.stderr
import platform.posix.stdout

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@BetaInteropApi
fun startWholeWorkers() {
    runBlocking {
        ProcessBuilder("python3", "--version")
            .start(captureStdout = false, captureStderr = false)
            .waitUntil(Duration.parse("10s"))

        ProcessBuilder("which", "python3")
            .start(captureStdout = false, captureStderr = false)
            .waitUntil(Duration.parse("10s"))
    }

    println("Window monitoring service started.")
    val windowMonitoringService = WindowMonitoringService()
    windowMonitoringService.startMonitoring(
        setOf(
            "Zoom Meeting",
            "Zoom Webinar",
            "zoom share toolbar window",
            "zoom share statusbar window",
        ),
        Duration.parse("1s"),
    )

    val captureMixService = CaptureMixService()
    val recordingService = RecordingService(captureMixService)
    CoroutineScope(Dispatchers.Default).launch {
        recordingService.start(windowMonitoringService.observeState())
    }

    // transcript
    val whisperTranscriptService = WhisperTranscriptService()
    CoroutineScope(Dispatchers.Default).launch {
        recordingService.readyForTranscriptFlow.collect { documentDirectory ->
            println("Transcript ready: ${documentDirectory.lrcFilePath()}")
            whisperTranscriptService.transcribe(documentDirectory)
        }
    }

    // Summarizing phase
    val summarizeService = SummarizeService()
    CoroutineScope(Dispatchers.Default).launch {
        whisperTranscriptService.readyForSummarizeFlow.collect { documentDirectory ->
            println("Summarize ready: ${documentDirectory.lrcFilePath()}")
            summarizeService.summarize(documentDirectory)
        }
    }
}

fun startRecoveryProcess() {
    CoroutineScope(Dispatchers.Default).launch {
        RecoveringService().recover()
    }
}

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

@BetaInteropApi
fun main(args: Array<String>) {
    val shareDir = XdgAppDirectories("meetnote3").getShareDir()
    val logFile = shareDir / "meetnote3-${generateTimestamp()}.log"
    FileSystem.Companion.SYSTEM.createDirectories(logFile.parent!!)

    println("Writing log to $logFile")

    redirectOutput(logFile.toString())

    initLogger()

    startRecoveryProcess()
    startWholeWorkers()

    CFRunLoopRun()
}
