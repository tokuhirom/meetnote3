package meetnote3.ui

import meetnote3.info
import platform.AppKit.NSApplicationDelegateProtocol
import platform.AppKit.NSMenu
import platform.AppKit.NSMenuItem
import platform.AppKit.NSStatusBar
import platform.AppKit.NSStatusItem
import platform.AppKit.NSVariableStatusItemLength
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject
import platform.posix.system

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction

class TrayIconHandler {
    // keep these properties as fields to avoid being garbage collected.
    lateinit var appDelegate: NSApplicationDelegateProtocol
    lateinit var statusItem: NSStatusItem

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun startTrayIcon(serverPort: Int): NSApplicationDelegateProtocol {
        appDelegate =
            object : NSObject(), NSApplicationDelegateProtocol {
                override fun applicationDidFinishLaunching(notification: NSNotification) {
                    info("Application did finish launching")
                    statusItem =
                        NSStatusBar.systemStatusBar.statusItemWithLength(NSVariableStatusItemLength)
                    statusItem.button?.title = "Meetnote3"
                    val menu =
                        NSMenu().apply {
                            addItem(
                                NSMenuItem(
                                    "Open Browser",
                                    action = NSSelectorFromString("openBrowser"),
                                    keyEquivalent = "o",
                                ),
                            )
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

                @ObjCAction
                fun openBrowser() {
                    info("Open browser")
                    system("open http://localhost:$serverPort/")
                }
            }
        return appDelegate
    }
}
