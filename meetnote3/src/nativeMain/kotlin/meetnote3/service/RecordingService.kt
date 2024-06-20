package meetnote3.service

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import meetnote3.info
import meetnote3.model.DocumentDirectory

class RecordingService(
    private val captureMixService: CaptureMixService,
) {
    @Suppress("ktlint:standard:backing-property-naming")
    private val _recordingCompletionFlow = MutableSharedFlow<DocumentDirectory>()
    val readyForTranscriptFlow: SharedFlow<DocumentDirectory> = _recordingCompletionFlow.asSharedFlow()

    @OptIn(BetaInteropApi::class)
    suspend fun start(observeState: StateFlow<RecordingState>) {
        var recorder: CaptureState? = null

        observeState.collect { recordingState ->
            when (recordingState) {
                is RecordingState.Idle -> {
                    info("Starting idle state...")

                    // stop recording if it's started...
                    if (recorder != null) {
                        recorder?.stop()
                        _recordingCompletionFlow.emit(recorder!!.documentDirectory)
                        recorder = null
                    }
                }

                is RecordingState.Recording -> {
                    info("Recording")

                    val documentDirectory = DocumentDirectory.create()

                    recorder = captureMixService.start(documentDirectory)
                }
            }
        }
    }
}
