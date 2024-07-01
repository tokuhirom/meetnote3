package meetnote3.service

import platform.posix.warn

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WholeWorkersFactoryService(
    private val summarizeService: SummarizeService,
) {
    @BetaInteropApi
    fun runAll() {
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
                try {
                    whisperTranscriptService.transcribe(documentDirectory)
                } catch (e: Exception) {
                    warn("Cannot transcribe $documentDirectory: $e")
                }
            }
        }

        // Summarizing phase
        CoroutineScope(Dispatchers.Default).launch {
            whisperTranscriptService.readyForSummarizeFlow.collect { documentDirectory ->
                println("Summarize ready: ${documentDirectory.lrcFilePath()}")
                try {
                    summarizeService.summarize(documentDirectory)
                } catch (e: Exception) {
                    warn("Cannot summarize $documentDirectory: $e")
                }
            }
        }
    }
}
