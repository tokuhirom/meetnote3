import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import meetnote3.initLogger
import meetnote3.service.CaptureMixService
import meetnote3.service.RecordingService
import meetnote3.service.WindowMonitoringService
import platform.CoreFoundation.CFRunLoopRun
import kotlin.time.Duration

@BetaInteropApi
fun main(args: Array<String>) {
    // start capturing worker

    initLogger()

    println("Window monitoring service started.")
    val windowMonitoringService = WindowMonitoringService()
    windowMonitoringService.startMonitoring(
        setOf(
            "Zoom Meeting",
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

    CFRunLoopRun()
}
