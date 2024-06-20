package meetnote3.service

import kotlinx.coroutines.flow.StateFlow
import meetnote3.info

class RecordingService {
    suspend fun start(observeState: StateFlow<RecordingState>) {
        observeState.collect { recordingState ->
            when (recordingState) {
                is RecordingState.Idle -> {
                    info("Starting idle state...")
                    // stop recording if it's started...
                }

                is RecordingState.Recording -> {
                    info("Recording")
                    // start recording...
                }
            }
        }
    }
}
