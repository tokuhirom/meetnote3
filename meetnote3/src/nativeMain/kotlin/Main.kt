import meetnote3.info
import meetnote3.initLogger
import meetnote3.service.EnvironmentDiagnosticService
import meetnote3.service.RecoveringService
import meetnote3.service.SummarizeService
import meetnote3.service.WholeWorkersFactoryService
import meetnote3.ui.TrayIconHandler
import meetnote3.utils.createNewSystemLogPath
import meetnote3.utils.getChildProcs
import meetnote3.utils.redirectOutput
import platform.AppKit.NSApplication
import platform.posix.getenv

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalForeignApi::class)
@BetaInteropApi
fun main() {
    getenv("MEETNOTE3_PORT")
    val summarizeService = SummarizeService()

    val systemLogPath = createNewSystemLogPath()

    println("Writing log to $systemLogPath")

    if (getenv("MEETNOTE3_NO_REDIRECT_LOG") != null) {
        println("Debug mode is enabled.")
    } else {
        redirectOutput(systemLogPath.toString())
    }

    initLogger()

    EnvironmentDiagnosticService().show()

    CoroutineScope(Dispatchers.Default).launch {
        RecoveringService(summarizeService).recover()
    }

    WholeWorkersFactoryService(summarizeService).runAll()

    CoroutineScope(Dispatchers.Default).launch {
        println("Showing child processes...")
        while (true) {
            getChildProcs()
            delay(Duration.parse("1m"))
        }
    }

    val app = NSApplication.sharedApplication()

    info("Registering tray icon...")
    val trayIconHandler = TrayIconHandler()
    val appDelegate = trayIconHandler.startTrayIcon()
    app.delegate = appDelegate
    app.run()
    error("Should not reach here.")
}
