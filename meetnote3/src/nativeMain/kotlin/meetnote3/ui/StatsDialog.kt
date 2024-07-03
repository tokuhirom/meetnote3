package meetnote3.ui

import meetnote3.info
import meetnote3.utils.getChildProcs
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSScrollView
import platform.AppKit.NSTextView
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowDelegateProtocol
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskResizable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.AppKit.translatesAutoresizingMaskIntoConstraints
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.darwin.NSObject

import kotlinx.cinterop.ExperimentalForeignApi

class StatsDialog :
    NSObject(),
    NSWindowDelegateProtocol {
    private var window: NSWindow? = null
    private lateinit var logBodyTextView: NSTextView

    // Note: instanceHolder cause the memory leak.
    //
    // Although this approach is not ideal, due to Kotlin/Native's memory management, the Window object can be
    // unexpectedly deallocated, causing crashes. Therefore, I am currently implementing this workaround. If
    // anyone knows a proper fix for this issue, please let me know!
    private val instanceHolder = mutableListOf<NSWindow>()

    fun show() {
        if (window == null) {
            window = createWindow()
        }
        window?.makeKeyAndOrderFront(null)
        window?.orderFrontRegardless()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createWindow(): NSWindow {
        val window = NSWindow(
            contentRect = NSMakeRect(0.0, 0.0, 480.0, 360.0),
            styleMask = NSWindowStyleMaskTitled or NSWindowStyleMaskClosable or NSWindowStyleMaskResizable,
            backing = NSBackingStoreBuffered,
            defer = false,
        )
        window.title = "System Log Viewer"
        window.delegate = this

        val contentView = window.contentView
        logBodyTextView = NSTextView(NSMakeRect(10.0, 10.0, 460.0, 300.0)).apply {
            setEditable(false)
        }
        logBodyTextView.string = getChildProcs()
            .joinToString("\n") {
                it.toString()
            }

        contentView?.addSubview(
            NSScrollView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                documentView = logBodyTextView
                setFrame(NSMakeRect(10.0, 10.0, 460.0, 300.0))
            },
        )

        instanceHolder.add(window)
        return window
    }

    override fun windowWillClose(notification: NSNotification) {
        info("Window will close")
        window?.delegate = null
        window = null
    }
}
