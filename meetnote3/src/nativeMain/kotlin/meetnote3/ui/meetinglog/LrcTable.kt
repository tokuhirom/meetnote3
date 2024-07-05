package meetnote3.ui.meetinglog

import meetnote3.transcript.LrcLine
import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableView
import platform.AppKit.NSTableViewDataSourceProtocol
import platform.AppKit.NSTableViewDelegateProtocol
import platform.AppKit.NSView
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.darwin.NSObject

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

class LrcTableViewDelegate :
    NSObject(),
    NSTableViewDelegateProtocol,
    NSTableViewDataSourceProtocol {
    private var lrcItems: List<LrcLine> = listOf()

    fun updateLrcItems(newItems: List<LrcLine>) {
        lrcItems = newItems
    }

    override fun numberOfRowsInTableView(tableView: NSTableView): Long = lrcItems.size.toLong()

    @OptIn(ExperimentalForeignApi::class)
    override fun tableView(
        tableView: NSTableView,
        viewForTableColumn: NSTableColumn?,
        row: Long,
    ): NSView? {
        val cellView = tableView.makeViewWithIdentifier("LrcTableCellView", owner = this) as? LrcTableCellView
            ?: LrcTableCellView(
                frame = NSMakeRect(
                    0.0,
                    0.0,
                    tableView.bounds.useContents { this.size.width },
                    100.0,
                ),
            ).apply {
                identifier = "LrcTableCellView"
            }

        val item = lrcItems[row.toInt()]

        cellView.textField?.stringValue = item.timestamp + " " + item.content

        return cellView
    }

    override fun tableViewSelectionDidChange(notification: NSNotification) {
        val tableView = (notification.`object` as? NSTableView) ?: return
        val selectedRow = tableView.selectedRow
        if (selectedRow != -1L) {
//            val selectedFile = lrcItems[selectedRow.toInt()]
//            parent.setDocument(selectedFile.documentDirectory)
        }
    }

    override fun tableView(
        tableView: NSTableView,
        heightOfRow: Long,
    ): Double = 50.0
}
