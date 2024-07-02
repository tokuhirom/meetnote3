package meetnote3.ui

import meetnote3.info
import meetnote3.model.DocumentDirectory
import okio.FileSystem
import okio.IOException
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
    private lateinit var lrcBodyTextView: NSTextView

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
        val notesItems = DocumentDirectory
            .listAll()
            .sortedByDescending { it.shortName() }
            .take(50)
            .map {
                it.basedir.name + " " + it.duration() + " " + if (FileSystem.SYSTEM.exists(it.summaryFilePath())) {
                    "✅"
                } else {
                    "❌"
                }
            }
        val notesFilesDropdown = NSPopUpButton(NSMakeRect(10.0, 680.0, 300.0, 30.0), false).apply {
            addItemsWithTitles(notesItems)
            setEnabled(true)
            setTarget(this@MeetingLogDialog)
            setAction(NSSelectorFromString("notesFileSelected:"))
        }

        notesBodyTextView = NSTextView(NSMakeRect(320.0, 10.0, 630.0, 320.0)).apply {
            setEditable(false)
        }
        lrcBodyTextView = NSTextView(NSMakeRect(320.0, 340.0, 630.0, 320.0)).apply {
            setEditable(false)
        }

        notesItems.firstOrNull()?.let {
            setDocument(it)
        }

        contentView?.addSubview(notesFilesDropdown)
        contentView?.addSubview(
            NSScrollView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                documentView = notesBodyTextView
                setFrame(NSMakeRect(320.0, 340.0, 630.0, 320.0))
            },
        )
        contentView?.addSubview(
            NSScrollView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                documentView = lrcBodyTextView
                setFrame(NSMakeRect(320.0, 10.0, 630.0, 320.0))
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
        setDocument(selectedNotesFile)
    }

    private fun extractNameFromTitle(input: String): String {
        val indexOfFirstSpace = input.indexOf(' ')
        return if (indexOfFirstSpace != -1) {
            input.substring(0, indexOfFirstSpace)
        } else {
            input // スペースがない場合、元の文字列をそのまま返す
        }
    }

    fun setDocument(name: String) {
        val document = DocumentDirectory.find(extractNameFromTitle(name))
        if (document != null) {
            notesBodyTextView.string = readSummaryFile(document)
            lrcBodyTextView.string = readLrcFile(document)
        } else {
            notesBodyTextView.string = "Document not found."
            lrcBodyTextView.string = "Document not found."
        }
    }

    private fun readSummaryFile(document: DocumentDirectory): String {
        info("Load notes file: $document")

        return try {
            FileSystem.SYSTEM.read(document.summaryFilePath()) {
                readUtf8()
            }
        } catch (e: IOException) {
            "Cannot read summary file(${e.message}): ${document.summaryFilePath()}"
        }
    }

    private fun readLrcFile(document: DocumentDirectory): String {
        info("Load notes file: $document")

        return try {
            FileSystem.SYSTEM.read(document.lrcFilePath()) {
                readUtf8()
            }
        } catch (e: IOException) {
            "Cannot read lrc file(${e.message}): ${document.lrcFilePath()}"
        }
    }
}
