import meetnote3.info
import meetnote3.initLogger
import meetnote3.model.generateTimestamp
import meetnote3.service.EnvironmentDiagnosticService
import meetnote3.service.RecoveringService
import meetnote3.service.WholeWorkersFactoryService
import meetnote3.utils.XdgAppDirectories
import meetnote3.utils.redirectOutput
import okio.FileSystem
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationDelegateProtocol
import platform.AppKit.NSMenu
import platform.AppKit.NSMenuItem
import platform.AppKit.NSStatusBar
import platform.AppKit.NSVariableStatusItemLength
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun startTrayIcon() {
    autoreleasepool {
        val app = NSApplication.sharedApplication()

        val appDelegate =
            object : NSObject(), NSApplicationDelegateProtocol {
                override fun applicationDidFinishLaunching(notification: NSNotification) {
                    info("Application did finish launching")
                    val statusItem =
                        NSStatusBar.systemStatusBar.statusItemWithLength(NSVariableStatusItemLength)
                    statusItem.button?.title = "Meetnote3"
                    val menu =
                        NSMenu().apply {
                            addItem(
                                NSMenuItem(
                                    "Quit",
                                    action = NSSelectorFromString("terminate:"),
                                    keyEquivalent = "q",
                                ),
                            )
                        }
                    statusItem.menu = menu
                }
            }

        app.delegate = appDelegate
        app.run()
    }
}

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
