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
    private lateinit var appDelegate: NSApplicationDelegateProtocol
    private lateinit var statusItem: NSStatusItem
    private var systemLogDialog: SystemLogDialog? = null

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun startTrayIcon(serverPort: Int): NSApplicationDelegateProtocol {
        appDelegate = object : NSObject(), NSApplicationDelegateProtocol {
            override fun applicationDidFinishLaunching(notification: NSNotification) {
                info("Application did finish launching")
                statusItem = NSStatusBar.systemStatusBar.statusItemWithLength(NSVariableStatusItemLength)
                statusItem.button?.title = "Meetnote3"
                val menu = NSMenu().apply {
                    addItem(
                        NSMenuItem(
                            "Open Browser",
                            action = NSSelectorFromString("openBrowser"),
                            keyEquivalent = "o",
                        ),
                    )
                    addItem(
                        NSMenuItem(
                            "Open System Log Viewer",
                            action = NSSelectorFromString("openSystemLogDialog"),
                            keyEquivalent = "s",
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

            @ObjCAction
            fun openSystemLogDialog() {
                if (systemLogDialog == null) {
                    systemLogDialog = SystemLogDialog()
                }
                systemLogDialog?.show()
            }
        }
        return appDelegate
    }
}
