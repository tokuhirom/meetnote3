package meetnote3.ui

import meetnote3.info
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
