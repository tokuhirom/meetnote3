package meetnote3.ui

import meetnote3.info
import meetnote3.utils.listSystemLogFiles
import okio.FileSystem
import okio.Path
import platform.AppKit.NSApplicationDelegateProtocol
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSMenu
import platform.AppKit.NSMenuItem
import platform.AppKit.NSPopUpButton
import platform.AppKit.NSScrollView
import platform.AppKit.NSStatusBar
import platform.AppKit.NSStatusItem
import platform.AppKit.NSTextView
import platform.AppKit.NSVariableStatusItemLength
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskResizable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.AppKit.translatesAutoresizingMaskIntoConstraints
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject
import platform.posix.system

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction

class TrayIconHandler {
    // keep these properties as fields to avoid being garbage collected.
    private lateinit var appDelegate: NSApplicationDelegateProtocol
    private lateinit var statusItem: NSStatusItem

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
                    doOpenSystemLogDialog()
                }
            }
        return appDelegate
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun doOpenSystemLogDialog() {
        val window = NSWindow(
            contentRect = NSMakeRect(0.0, 0.0, 480.0, 360.0),
            styleMask = NSWindowStyleMaskTitled or NSWindowStyleMaskClosable or NSWindowStyleMaskResizable,
            backing = NSBackingStoreBuffered,
            defer = false,
        )
        window.title = "System Log Viewer"
        lateinit var logBodyTextView: NSTextView

        val handler =
            object : NSObject(), NSApplicationDelegateProtocol {
                @ObjCAction
                fun logFileSelected(sender: NSPopUpButton) {
                    info("Selected log file!!!")
                    val selectedLogFile = sender.titleOfSelectedItem ?: return
                    logBodyTextView.string = readLogFile(selectedLogFile)
                }
            }

        val contentView = window.contentView
        var logFilesDropdown = NSPopUpButton(NSMakeRect(10.0, 320.0, 460.0, 30.0), false).apply {
            addItemsWithTitles(
                listSystemLogFiles()
                    .sortedByDescending {
                        it.name
                    }.take(5)
                    .map { it.name },
            )
            setEnabled(true)
            setTarget(handler)
            setAction(NSSelectorFromString("logFileSelected:"))
        }

        val scrollView = NSScrollView().apply {
            translatesAutoresizingMaskIntoConstraints = false
            documentView = NSTextView().apply {
                setEditable(false)
                string = "Hello, world"
            }
        }
        logBodyTextView = NSTextView(NSMakeRect(10.0, 10.0, 460.0, 300.0)).apply {
            setEditable(false)
        }

        contentView?.addSubview(logFilesDropdown)
        contentView?.addSubview(
            NSScrollView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                documentView = logBodyTextView
                setFrame(NSMakeRect(10.0, 10.0, 460.0, 300.0))
            },
        )

        window.contentView?.addSubview(scrollView)
        window.makeKeyAndOrderFront(null)
        window.orderFrontRegardless()
    }

    fun readLogFile(systemLog: String): String {
        val systemLogFile: Path? = listSystemLogFiles().find { it.name == systemLog }
        if (systemLogFile != null) {
            FileSystem.SYSTEM.read(systemLogFile) {
                return readUtf8()
            }
        } else {
            error("System log file not found.")
        }
    }
}
