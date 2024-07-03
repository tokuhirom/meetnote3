package meetnote3.ui

import meetnote3.info
import meetnote3.utils.getChildProcs
import meetnote3.workers.SummarizingWorker
import meetnote3.workers.TranscriptWorker
import okio.FileSystem
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSScrollView
import platform.AppKit.NSTextView
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowDelegateProtocol
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskResizable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.AppKit.bottomAnchor
import platform.AppKit.leadingAnchor
import platform.AppKit.topAnchor
import platform.AppKit.trailingAnchor
import platform.AppKit.translatesAutoresizingMaskIntoConstraints
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.Foundation.NSTimer
import platform.darwin.NSObject

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class StatsDialog(
    private val summarizingWorker: SummarizingWorker,
    private val transcriptWorker: TranscriptWorker,
) : NSObject(),
    NSWindowDelegateProtocol {
    private var window: NSWindow? = null
    private lateinit var logBodyTextView: NSTextView
    private var timer: NSTimer? = null

    // Note: instanceHolder causes a memory leak.
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
        startTimer()
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

        val contentView = window.contentView!!
        logBodyTextView = NSTextView().apply {
            setEditable(false)
        }
        updateLog()

        val scrollView = NSScrollView().apply {
            translatesAutoresizingMaskIntoConstraints = false
            documentView = logBodyTextView
            hasVerticalScroller = true
            hasHorizontalScroller = true
        }

        contentView.addSubview(scrollView)

        // Add constraints to make the scroll view adjust to the window size
        scrollView.leadingAnchor.constraintEqualToAnchor(contentView.leadingAnchor).active = true
        scrollView.trailingAnchor.constraintEqualToAnchor(contentView.trailingAnchor).active = true
        scrollView.topAnchor.constraintEqualToAnchor(contentView.topAnchor).active = true
        scrollView.bottomAnchor.constraintEqualToAnchor(contentView.bottomAnchor).active = true

        instanceHolder.add(window)
        return window
    }

    private fun updateLog() {
        logBodyTextView.string = StringBuilder()
            .apply {
                append(
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toString(),
                )
                append("\n\n")
                append("# Child Processes\n")
                append(
                    getChildProcs()
                        .joinToString("\n") {
                            "" + it.pid + " " + it.name
                        },
                )
                append("\n\n")
                append("# Summarizing\n")
                summarizingWorker.processingDocument()?.let {
                    append(it.shortName())
                }
                append("\n\n")
                append("# Transcribing\n\n")
                transcriptWorker.processLogs().forEach {
                    append(it.documentDirectory.basedir)
                    append("\n")
                    if (it.endAt != null) {
                        append("  Done(")
                        append(((it.endAt!! - it.startAt) / 1000).toString())
                        append("s)")
                    } else {
                        append("  Processing(")
                        append(((Clock.System.now().toEpochMilliseconds() - it.startAt) / 1000).toString())
                        append("s) ")
                        append(
                            FileSystem.SYSTEM
                                .metadataOrNull(it.documentDirectory.mixedFilePath())
                                ?.size
                                ?.let { size ->
                                    (size / 1024 / 1024).toString() + "MiB"
                                }.toString(),
                        )
                    }
                    if (it.error != null) {
                        append(" ")
                        append(it.error)
                    }
                    append("\n\n")
                }
                append("\n\n  Waiting count: ${transcriptWorker.waitingCount()}\n")
                append("\n")
            }.toString()
    }

    private fun startTimer() {
        timer = NSTimer.scheduledTimerWithTimeInterval(
            // Update every 5 seconds
            5.0,
            repeats = true,
            block = { updateLog() },
        )
    }

    private fun stopTimer() {
        timer?.invalidate()
        timer = null
    }

    override fun windowWillClose(notification: NSNotification) {
        info("Window will close")
        stopTimer()
        window?.delegate = null
        window = null
    }
}
