import meetnote3.info
import meetnote3.initLogger
import meetnote3.server.Server
import meetnote3.service.EnvironmentDiagnosticService
import meetnote3.service.RecoveringService
import meetnote3.service.WholeWorkersFactoryService
import meetnote3.ui.startTrayIcon
import meetnote3.utils.createNewSystemLogPath
import meetnote3.utils.getChildProcs
import meetnote3.utils.redirectOutput

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@BetaInteropApi
fun main(args: Array<String>) {
    val systemLogPath = createNewSystemLogPath()

    println("Writing log to $systemLogPath")

    redirectOutput(systemLogPath.toString())

    initLogger()

    EnvironmentDiagnosticService().show()

    CoroutineScope(Dispatchers.Default).launch {
        RecoveringService().recover()
    }

    WholeWorkersFactoryService().runAll()

    val port = Server().startServer()
    info("Server started at http://localhost:$port/")

    CoroutineScope(Dispatchers.Default).launch {
        println("Showing child processes...")
        while (true) {
            getChildProcs()
            delay(Duration.parse("1m"))
        }
    }

    info("Registering tray icon...")
    startTrayIcon(port)
}
