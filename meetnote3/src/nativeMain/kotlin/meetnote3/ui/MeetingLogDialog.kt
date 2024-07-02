package meetnote3.ui

import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.service.listMeetingLogs
import okio.FileSystem
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSPopUpButton
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
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction

class MeetingLogDialog :
    NSObject(),
    NSWindowDelegateProtocol {
    private var window: NSWindow? = null
    private lateinit var notesBodyTextView: NSTextView

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
            contentRect = NSMakeRect(0.0, 0.0, 960.0, 720.0),
            styleMask = NSWindowStyleMaskTitled or NSWindowStyleMaskClosable or NSWindowStyleMaskResizable,
            backing = NSBackingStoreBuffered,
            defer = false,
        )
        window.title = "Meeting Notes Viewer"
        window.delegate = this

        val contentView = window.contentView
        val notesItems = listMeetingLogs().sortedByDescending { it.name }.take(5).map { it.name }
        val notesFilesDropdown = NSPopUpButton(NSMakeRect(10.0, 680.0, 300.0, 30.0), false).apply {
            addItemsWithTitles(notesItems)
            setEnabled(true)
            setTarget(this@MeetingLogDialog)
            setAction(NSSelectorFromString("notesFileSelected:"))
        }

        notesBodyTextView = NSTextView(NSMakeRect(320.0, 10.0, 630.0, 660.0)).apply {
            setEditable(false)
        }
        notesItems.firstOrNull()?.let {
            notesBodyTextView.string = readNotesFile(it)
        }

        contentView?.addSubview(notesFilesDropdown)
        contentView?.addSubview(
            NSScrollView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                documentView = notesBodyTextView
                setFrame(NSMakeRect(320.0, 10.0, 630.0, 660.0))
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

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun notesFileSelected(sender: NSPopUpButton) {
        info("Selected notes file")
        val selectedNotesFile = sender.titleOfSelectedItem ?: return
        notesBodyTextView.string = readNotesFile(selectedNotesFile)
    }

    private fun readNotesFile(notesFile: String): String {
        info("Load notes file: $notesFile")
        val document = DocumentDirectory.find(notesFile!!)
        return if (document != null) {
            FileSystem.SYSTEM.read(document.lrcFilePath()) {
                readUtf8()
            }
        } else {
            "Meeting notes file not found."
        }
    }
}
