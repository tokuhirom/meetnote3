import meetnote3.info
import meetnote3.initLogger
import meetnote3.server.Server
import meetnote3.service.EnvironmentDiagnosticService
import meetnote3.service.RecoveringService
import meetnote3.service.WholeWorkersFactoryService
import meetnote3.ui.TrayIconHandler
import meetnote3.utils.createNewSystemLogPath
import meetnote3.utils.getChildProcs
import meetnote3.utils.redirectOutput
import platform.AppKit.NSApplication

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@BetaInteropApi
fun main(args: Array<String>) {
    val port = Server().startServer()
    info("Server started at http://localhost:$port/")

    val systemLogPath = createNewSystemLogPath()

    println("Writing log to $systemLogPath")

    redirectOutput(systemLogPath.toString())

    initLogger()

    info("Server is ready: http://localhost:$port/")

    EnvironmentDiagnosticService().show()

    CoroutineScope(Dispatchers.Default).launch {
        RecoveringService().recover()
    }

    WholeWorkersFactoryService().runAll()

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
    val appDelegate = trayIconHandler.startTrayIcon(serverPort = port)
    app.delegate = appDelegate
    app.run()
    error("Should not reach here.")
}
