package meetnote3.service

import meetnote3.ui.TrayIconHandler
import meetnote3.workers.SummarizingWorker
import meetnote3.workers.TranscriptWorker
import platform.AppKit.NSApplicationDelegateProtocol

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Container {
    private val summarizeService = SummarizeService()
    private val captureMixService = CaptureMixService()
    private val summarizingWorker = SummarizingWorker(summarizeService)
    private val whisperTranscriptService = WhisperTranscriptService(summarizingWorker)
    private val transcriptWorker = TranscriptWorker(whisperTranscriptService)
    private val recordingService = RecordingService(captureMixService, transcriptWorker)
    private val recoveringService = RecoveringService(whisperTranscriptService, summarizeService)
    private val trayIconHandler = TrayIconHandler(
        summarizingWorker,
        transcriptWorker,
        recoveringService,
        recordingService,
    )

    fun startTrayIcon(): NSApplicationDelegateProtocol = trayIconHandler.startTrayIcon()

    @OptIn(BetaInteropApi::class)
    private val windowMonitoringService = WindowMonitoringService().apply {
        startMonitoring(
            setOf(
                "Zoom Meeting",
                "Zoom Webinar",
                "zoom share toolbar window",
                "zoom share statusbar window",
            ),
            Duration.parse("1s"),
        )
    }

    @BetaInteropApi
    fun runAllWorkers() {
        println("Window monitoring service started.")

        CoroutineScope(Dispatchers.Default).launch {
            recoveringService.recover()
        }

        CoroutineScope(Dispatchers.Default).launch {
            recordingService.start(windowMonitoringService.observeState())
        }

        transcriptWorker.start()
        summarizingWorker.start()
    }
}
