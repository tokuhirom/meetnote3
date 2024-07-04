package meetnote3.ui

import meetnote3.info
import meetnote3.service.RecordingService
import meetnote3.service.RecoveringService
import meetnote3.ui.meetinglog.MeetingLogDialog
import meetnote3.workers.SummarizingWorker
import meetnote3.workers.TranscriptWorker
import platform.AppKit.NSApplicationDelegateProtocol
import platform.AppKit.NSMenu
import platform.AppKit.NSMenuItem
import platform.AppKit.NSStatusBar
import platform.AppKit.NSStatusItem
import platform.AppKit.NSVariableStatusItemLength
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimer
import platform.darwin.NSObject

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction

class TrayIconHandler(
    private val summarizingWorker: SummarizingWorker,
    private val transcriptWorker: TranscriptWorker,
    private val recoveringService: RecoveringService,
    private val recordingService: RecordingService,
) {
    private lateinit var appDelegate: NSApplicationDelegateProtocol
    private lateinit var statusItem: NSStatusItem
    private var systemLogDialog: SystemLogDialog? = null
    private var meetingLogDialog: MeetingLogDialog? = null
    private var timer: NSTimer? = null

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun startTrayIcon(): NSApplicationDelegateProtocol {
        appDelegate = object : NSObject(), NSApplicationDelegateProtocol {
            override fun applicationDidFinishLaunching(notification: NSNotification) {
                info("Application did finish launching")
                statusItem = NSStatusBar.systemStatusBar.statusItemWithLength(NSVariableStatusItemLength)
                statusItem.button?.title = getTitle()
                val menu = NSMenu().apply {
                    addItem(
                        NSMenuItem(
                            "Open Meeting Log Viewer",
                            action = NSSelectorFromString("openMeetingLogDialog"),
                            keyEquivalent = "m",
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
                            "Open Stats Viewer",
                            action = NSSelectorFromString("openStatsDialog"),
                            keyEquivalent = "t",
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
                startTitleUpdateTimer()
            }

            @ObjCAction
            fun openSystemLogDialog() {
                if (systemLogDialog == null) {
                    systemLogDialog = SystemLogDialog()
                }
                systemLogDialog?.show()
            }

            @ObjCAction
            fun openMeetingLogDialog() {
                if (meetingLogDialog == null) {
                    meetingLogDialog = MeetingLogDialog()
                }
                meetingLogDialog?.show()
            }

            @ObjCAction
            fun openStatsDialog() {
                val statsDialog = StatsDialog(summarizingWorker, transcriptWorker, recoveringService)
                statsDialog.show()
            }
        }
        return appDelegate
    }

    private fun startTitleUpdateTimer() {
        timer = NSTimer.scheduledTimerWithTimeInterval(1.0, true) {
            statusItem.button?.title = getTitle()
        }
    }

    fun getTitle(): String = "Meetnote3" + if (recordingService.isRecording()) "!" else ""
}
