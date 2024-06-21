import meetnote3.initLogger
import meetnote3.service.CaptureMixService
import meetnote3.service.RecordingService
import meetnote3.service.SummarizeService
import meetnote3.service.WhisperTranscriptService
import meetnote3.service.WindowMonitoringService
import meetnote3.utils.ProcessBuilder
import platform.CoreFoundation.CFRunLoopRun

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@BetaInteropApi
fun startWholeWorkers() {
    runBlocking {
        ProcessBuilder("ffmpeg", "-version")
            .start(captureStdout = false, captureStderr = false)
            .waitUntil(Duration.parse("10s"))

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

@BetaInteropApi
fun main(args: Array<String>) {
    initLogger()

    startWholeWorkers()
//    startRecoveryProcess()

    CFRunLoopRun()
}
