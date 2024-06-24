import meetnote3.info
import meetnote3.initLogger
import meetnote3.model.generateTimestamp
import meetnote3.service.EnvironmentDiagnosticService
import meetnote3.service.RecoveringService
import meetnote3.service.WholeWorkersFactoryService
import meetnote3.ui.startTrayIcon
import meetnote3.utils.XdgAppDirectories
import meetnote3.utils.redirectOutput
import okio.FileSystem

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@BetaInteropApi
fun main(args: Array<String>) {
    val shareDir = XdgAppDirectories("meetnote3").getShareDir()
    val logFile = shareDir / "meetnote3-${generateTimestamp()}.log"
    FileSystem.Companion.SYSTEM.createDirectories(logFile.parent!!)

    println("Writing log to $logFile")

    redirectOutput(logFile.toString())

    initLogger()

    EnvironmentDiagnosticService().show()

    CoroutineScope(Dispatchers.Default).launch {
        RecoveringService().recover()
    }

    WholeWorkersFactoryService().runAll()

    info("Registering tray icon...")
    startTrayIcon()
}
