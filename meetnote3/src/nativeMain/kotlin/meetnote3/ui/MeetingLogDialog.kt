package meetnote3.ui

import meetnote3.info
import meetnote3.model.DocumentDirectory
import okio.FileSystem
import okio.IOException
import okio.Path
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSButton
import platform.AppKit.NSImage
import platform.AppKit.NSImageScaleProportionallyUpOrDown
import platform.AppKit.NSImageView
import platform.AppKit.NSPopUpButton
import platform.AppKit.NSScrollView
import platform.AppKit.NSTextAlignmentCenter
import platform.AppKit.NSTextField
import platform.AppKit.NSTextView
import platform.AppKit.NSView
import platform.AppKit.NSViewHeightSizable
import platform.AppKit.NSViewWidthSizable
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowDelegateProtocol
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskResizable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.AppKit.translatesAutoresizingMaskIntoConstraints
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSSize
import platform.Foundation.create
import platform.darwin.NSObject

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData =
    this.usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = this.size.toULong(),
        )
    }

class MeetingLogDialog :
    NSObject(),
    NSWindowDelegateProtocol {
    private var window: NSWindow? = null
    private lateinit var notesBodyTextView: NSTextView
    private lateinit var lrcBodyTextView: NSTextView
    private lateinit var imagesContainerView: NSView
    private lateinit var documentDirectory: DocumentDirectory

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

        imagesContainerView = NSView(NSMakeRect(10.0, 10.0, 10.0, 20.0)).apply {
            setAutoresizingMask(NSViewWidthSizable or NSViewHeightSizable)
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
        contentView?.addSubview(
            NSScrollView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                documentView = imagesContainerView
                setFrame(NSMakeRect(10.0, 10.0, 300.0, 320.0))
                hasHorizontalScroller = false
                hasVerticalScroller = true
            },
        )
        contentView?.addSubview(buildAudioPlayer())

        instanceHolder.add(window)
        return window
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun buildAudioPlayer(): NSView {
        val audioPlayer = AudioPlayer()

        val containerView = NSView(NSMakeRect(540.0, 500.0, 400.0, 200.0))

        val playButton = NSButton(NSMakeRect(50.0, 50.0, 80.0, 30.0))
        playButton.title = "Play"
        playButton.setTarget {
            if (FileSystem.SYSTEM.exists(documentDirectory.mixedFilePath())) {
                info("Play audio file: ${documentDirectory.mixedFilePath()}")
                audioPlayer.play(documentDirectory.mixedFilePath().toString())
            } else {
                info("Audio file not found: ${documentDirectory.mixedFilePath()}")
            }
        }

        val pauseButton = NSButton(NSMakeRect(150.0, 50.0, 80.0, 30.0))
        pauseButton.title = "Pause"
        pauseButton.setTarget {
            audioPlayer.pause()
        }

        val stopButton = NSButton(NSMakeRect(250.0, 50.0, 80.0, 30.0))
        stopButton.title = "Stop"
        stopButton.setTarget {
            audioPlayer.stop()
        }

        val currentTimeLabel = NSTextField(NSMakeRect(150.0, 100.0, 100.0, 30.0))
        currentTimeLabel.stringValue = "00:00"
        currentTimeLabel.setEditable(false)
        currentTimeLabel.setBezeled(true)
        currentTimeLabel.alignment = NSTextAlignmentCenter
        AudioPlayer.currentTimeLabel = currentTimeLabel

        val seekButton = NSButton(NSMakeRect(150.0, 150.0, 100.0, 30.0))
        seekButton.title = "Seek to 10:00"
        seekButton.setTarget {
            audioPlayer.seekToTime(600.0) // 10分 = 600秒
        }

        containerView.addSubview(playButton)
        containerView.addSubview(pauseButton)
        containerView.addSubview(stopButton)
        containerView.addSubview(currentTimeLabel)
        containerView.addSubview(seekButton)

        return containerView
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

    private fun setDocument(name: String) {
        val document = DocumentDirectory.find(extractNameFromTitle(name))
        if (document != null) {
            this.documentDirectory = document
            imagesContainerView.subviews.forEach {
                if (it is NSView) {
                    it.removeFromSuperview()
                }
            }
            var offset = 0.0
            document.listImages().forEach { path: Path ->
                info("Image path: $path")
                offset += displayImage(path, offset)
            }

            notesBodyTextView.string = readSummaryFile(document)
            lrcBodyTextView.string = readLrcFile(document)
        } else {
            notesBodyTextView.string = "Document not found."
            lrcBodyTextView.string = "Document not found."
            imagesContainerView.subviews.forEach {
                if (it is NSView) {
                    it.removeFromSuperview()
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun displayImage(
        path: Path,
        offset: Double,
    ): Double {
        try {
            val imageData = FileSystem.SYSTEM.read(path) {
                readByteArray()
            }
            val nsImage = NSImage(data = imageData.toNSData())
            val originalSize: CValue<NSSize> = nsImage.size
            val width = 300.0
            val height = originalSize
                .useContents {
                    (width / this.width) * this.height
                }.toDouble()
            val imageView = NSImageView(
                CGRectMake(
                    x = 0.0,
                    y = offset,
                    width = 300.0,
                    height = height,
                ),
            ).apply {
                image = nsImage
                imageScaling = NSImageScaleProportionallyUpOrDown
            }
            imagesContainerView.addSubview(imageView)
            return height
        } catch (e: IOException) {
            info("Cannot read image file(${e.message}): $path")
            return 0.0
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
