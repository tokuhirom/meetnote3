package meetnote3.service

import meetnote3.debug
import meetnote3.getSharableContent
import platform.ScreenCaptureKit.SCWindow

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RecordingState {
    data object Idle : RecordingState()

    data object Recording : RecordingState()
}

class WindowMonitoringService {
    private val stateFlow = MutableStateFlow<RecordingState>(RecordingState.Idle)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    @BetaInteropApi
    fun startMonitoring(
        titles: Set<String>,
        interval: Duration,
    ) {
        coroutineScope.launch {
            while (true) {
                val content = getSharableContent()

                val zoomWindows = content.windows
                    .mapNotNull { it as? SCWindow }
                    .filter { it.active && it.title != null }
                    .filter { it.owningApplication?.bundleIdentifier == "us.zoom.xos" }
                    .filter { titles.contains(it.title) }

                if (zoomWindows.isNotEmpty()) {
                    debug("Found Zoom windows: $zoomWindows")
                    stateFlow.value = RecordingState.Recording
                } else {
                    stateFlow.value = RecordingState.Idle
                }

                delay(interval)
            }
        }
    }

    fun observeState(): StateFlow<RecordingState> = stateFlow
}
