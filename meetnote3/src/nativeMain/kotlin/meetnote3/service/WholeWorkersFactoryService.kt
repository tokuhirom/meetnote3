package meetnote3.service

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
                whisperTranscriptService.transcribe(documentDirectory)
            }
        }

        // Summarizing phase
        CoroutineScope(Dispatchers.Default).launch {
            whisperTranscriptService.readyForSummarizeFlow.collect { documentDirectory ->
                println("Summarize ready: ${documentDirectory.lrcFilePath()}")
                summarizeService.summarize(documentDirectory)
            }
        }
    }
}
