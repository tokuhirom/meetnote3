import meetnote3.info
import meetnote3.initLogger
import meetnote3.service.Container
import meetnote3.service.EnvironmentDiagnosticService
import meetnote3.utils.createNewSystemLogPath
import meetnote3.utils.getChildProcs
import meetnote3.utils.redirectOutput
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
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

    val systemLogPath = createNewSystemLogPath()

    println("Writing log to $systemLogPath")

    if (getenv("MEETNOTE3_NO_REDIRECT_LOG") != null) {
        println("Debug mode is enabled.")
    } else {
        redirectOutput(systemLogPath.toString())
    }

    initLogger()

    EnvironmentDiagnosticService().show()

    val container = Container()
    container.runAllWorkers()

    CoroutineScope(Dispatchers.Default).launch {
        println("Showing child processes...")
        while (true) {
            getChildProcs()
            delay(Duration.parse("1m"))
        }
    }

    val app = NSApplication.sharedApplication()

    info("Registering tray icon...")
    val appDelegate = container.startTrayIcon()
    app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    app.activateIgnoringOtherApps(true)
    app.delegate = appDelegate
    app.run()
    error("Should not reach here.")
}
