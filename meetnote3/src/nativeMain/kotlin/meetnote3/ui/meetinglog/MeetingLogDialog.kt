import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.model.DocumentStatus
import meetnote3.transcript.LrcLine
import meetnote3.transcript.parseLrcContent
import meetnote3.ui.meetinglog.AudioPlayer
import meetnote3.ui.meetinglog.FileTableItem
import meetnote3.ui.meetinglog.FileTableViewDelegate
import meetnote3.ui.meetinglog.ImageTableItem
import meetnote3.ui.meetinglog.ImageTableViewDelegate
import meetnote3.ui.meetinglog.LrcTableViewDelegate
import okio.FileSystem
import okio.IOException
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSButton
import platform.AppKit.NSScrollView
import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableView
import platform.AppKit.NSTextAlignmentCenter
import platform.AppKit.NSTextField
import platform.AppKit.NSTextView
import platform.AppKit.NSView
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowDelegateProtocol
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskMiniaturizable
import platform.AppKit.NSWindowStyleMaskResizable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimer
import platform.darwin.NSObject

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction

class MeetingLogDialog :
    NSObject(),
    NSWindowDelegateProtocol {
    private var window: NSWindow? = null
    private lateinit var notesBodyTextView: NSTextView
    private lateinit var lrcTableView: NSTableView
    private lateinit var imageTableView: NSTableView
    private lateinit var documentDirectory: DocumentDirectory
    private val audioPlayer = AudioPlayer()
    private var reloadTimer: NSTimer? = null
    private var fileTableView: NSTableView? = null
    private val fileTableViewDelegate = FileTableViewDelegate(this)
    private val lrcTableViewDelegate = LrcTableViewDelegate(this)
    private val imageTableViewDelegate = ImageTableViewDelegate()

    private val instanceHolder = mutableListOf<NSWindow>()

    @OptIn(BetaInteropApi::class)
    private val audioPlayerController = object : NSObject() {
        @ObjCAction
        fun onPlayButtonClicked() {
            info("Start player...")
            if (FileSystem.SYSTEM.exists(documentDirectory.mixedFilePath())) {
                info("Play audio file: ${documentDirectory.mixedFilePath()}")
                audioPlayer.play()
            } else {
                info("Audio file not found: ${documentDirectory.mixedFilePath()}")
            }
        }

        @ObjCAction
        fun onPauseButtonClicked() {
            info("Pause player...")
            audioPlayer.pause()
        }
    }

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
            contentRect = NSMakeRect(0.0, 0.0, 1260.0, 720.0),
            styleMask =
                NSWindowStyleMaskTitled or NSWindowStyleMaskClosable or NSWindowStyleMaskResizable or NSWindowStyleMaskMiniaturizable,
            backing = NSBackingStoreBuffered,
            defer = false,
        ).apply {
            makeKeyAndOrderFront(null)
        }
        window.title = "Meeting Notes Viewer"
        window.delegate = this

        val contentView = window.contentView!!

        fileTableView = NSTableView().apply {
            val column = NSTableColumn("Files").apply {
                width = 300.0
                setEditable(false)
                setHeaderView(null)
            }
            addTableColumn(column)
            delegate = fileTableViewDelegate
            dataSource = fileTableViewDelegate
        }

        val fileScrollView = NSScrollView().apply {
            documentView = fileTableView
            setFrame(NSMakeRect(10.0, 10.0, 300.0, 660.0))
            hasVerticalScroller = true
        }

        contentView.addSubview(fileScrollView)

        notesBodyTextView = NSTextView(NSMakeRect(320.0, 10.0, 630.0, 320.0)).apply {
            setEditable(false)
        }
        val notesScrollView = NSScrollView().apply {
            documentView = notesBodyTextView
            setFrame(NSMakeRect(320.0, 10.0, 630.0, 320.0))
            hasVerticalScroller = true
        }

        // LRC テーブルビュー
        lrcTableView = NSTableView().apply {
            val column = NSTableColumn("LRC Content").apply {
                width = 630.0
                setEditable(false)
                setHeaderView(null)
            }
            addTableColumn(column)
            delegate = lrcTableViewDelegate
            dataSource = lrcTableViewDelegate
        }
        val lrcScrollView = NSScrollView().apply {
            documentView = lrcTableView
            setFrame(NSMakeRect(320.0, 340.0, 630.0, 320.0))
            hasVerticalScroller = true
        }

        imageTableView = NSTableView().apply {
            val column = NSTableColumn("Images").apply {
                width = 300.0
                setEditable(false)
                setHeaderView(null)
            }
            addTableColumn(column)
            delegate = imageTableViewDelegate
            dataSource = imageTableViewDelegate
        }
        val imageScrollView = NSScrollView().apply {
            documentView = imageTableView
            setFrame(NSMakeRect(960.0, 10.0, 300.0, 660.0)) // Positioned to the right
            hasVerticalScroller = true
        }

        contentView.addSubview(notesScrollView)
        contentView.addSubview(lrcScrollView)
        contentView.addSubview(imageScrollView)
        contentView.addSubview(buildAudioPlayer())

        reloadTimer = NSTimer.scheduledTimerWithTimeInterval(
            interval = 10.0,
            repeats = true,
            block = { reloadNotesItems() },
        )
        reloadNotesItems()

        instanceHolder.add(window)
        return window
    }

    private fun loadNotesItems(): List<FileTableItem> {
        val notesItems = DocumentDirectory
            .listAll()
            .sortedByDescending { it.shortName() }
            .take(50)
            .map {
                val status = it.status()
                val title = it.shortName() + "\n" + if (status == DocumentStatus.DONE) {
                    "✅"
                } else {
                    status
                } + (it.duration() ?: "")
                FileTableItem(title, it)
            }
        return notesItems
    }

    private fun reloadNotesItems() {
        val newItems = loadNotesItems()
        fileTableViewDelegate.updateFiles(newItems)
        fileTableView?.reloadData()
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun buildAudioPlayer(): NSView {
        val containerView = NSView(NSMakeRect(320.0, 680.0, 630.0, 40.0))

        val playButton = NSButton(NSMakeRect(10.0, 10.0, 80.0, 30.0)).apply {
            title = "Play"
            target = audioPlayerController
            action = NSSelectorFromString("onPlayButtonClicked")
        }

        val pauseButton = NSButton(NSMakeRect(100.0, 10.0, 80.0, 30.0)).apply {
            title = "Pause"
            target = audioPlayerController
            action = NSSelectorFromString("onPauseButtonClicked")
        }

        val currentTimeLabel = NSTextField(NSMakeRect(280.0, 10.0, 80.0, 30.0)).apply {
            stringValue = "00:00"
            setEditable(false)
            setBezeled(true)
            alignment = NSTextAlignmentCenter
        }
        AudioPlayer.currentTimeLabel = currentTimeLabel

        containerView.addSubview(playButton)
        containerView.addSubview(pauseButton)
        containerView.addSubview(currentTimeLabel)

        return containerView
    }

    override fun windowWillClose(notification: NSNotification) {
        info("Window will close")
        audioPlayer.stop()
        window?.delegate = null
        window = null
    }

    fun setDocument(document: DocumentDirectory) {
        this.documentDirectory = document

        audioPlayer.pause()
        audioPlayer.load(documentDirectory.mixedFilePath().toString())

        reloadImages(document)

        notesBodyTextView.string = readSummaryFile(document)
        reloadLrcTable(document)
    }

    private fun reloadImages(document: DocumentDirectory) {
        val imageItems = document.listImages().map { ImageTableItem(it) }
        imageTableViewDelegate.updateImages(imageItems)
        imageTableView.reloadData()
    }

    private fun reloadLrcTable(document: DocumentDirectory) {
        val lrcItems = readLrcFile(document)
        lrcTableViewDelegate.updateLrcItems(lrcItems)
        lrcTableView.reloadData()
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

    private fun readLrcFile(document: DocumentDirectory): List<LrcLine> {
        info("Load notes file: $document")

        return try {
            FileSystem.SYSTEM.read(document.lrcFilePath()) {
                parseLrcContent(readUtf8())
            }
        } catch (e: IOException) {
            listOf(
                LrcLine("00:00.00", "Cannot read lrc file(${e.message}): ${document.lrcFilePath()}"),
            )
        }
    }

    fun seek(seconds: Double) {
        audioPlayer.seekToTime(seconds)
    }
}
