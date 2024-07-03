package meetnote3.service

import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.workers.TranscriptWorker

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.flow.StateFlow

class RecordingService(
    private val captureMixService: CaptureMixService,
    private val transcriptWorker: TranscriptWorker,
) {
    private var recorder: CaptureState? = null

    @OptIn(BetaInteropApi::class)
    suspend fun start(observeState: StateFlow<RecordingState>) {
        observeState.collect { recordingState ->
            when (recordingState) {
                is RecordingState.Idle -> {
                    info("Starting idle state...")

                    // stop recording if it's started...
                    recorder?.let {
                        it.stop()
                        transcriptWorker.emit(it.documentDirectory)
                        recorder = null
                    }
                }

                is RecordingState.Recording -> {
                    info("Recording")

                    try {
                        val documentDirectory = DocumentDirectory.create()

                        recorder = captureMixService.start(documentDirectory)
                    } catch (e: Exception) {
                        error("Failed to start recording: $e")
                    }
                }
            }
        }
    }

    fun isRecording() = recorder != null
}
