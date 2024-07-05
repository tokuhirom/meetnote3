package meetnote3.ui.meetinglog

import MeetingLogDialog
import meetnote3.model.DocumentDirectory
import platform.AppKit.NSImage
import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableView
import platform.AppKit.NSTableViewDataSourceProtocol
import platform.AppKit.NSTableViewDelegateProtocol
import platform.AppKit.NSView
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.Foundation.NSURL
import platform.darwin.NSObject

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

data class FileTableItem(
    val name: String,
    val documentDirectory: DocumentDirectory,
)

class FileTableViewDelegate(
    private val parent: MeetingLogDialog,
) : NSObject(),
    NSTableViewDelegateProtocol,
    NSTableViewDataSourceProtocol {
    private val files = mutableListOf<FileTableItem>()

    fun updateFiles(newFiles: List<FileTableItem>) {
        files.clear()
        files.addAll(newFiles)
    }

    override fun numberOfRowsInTableView(tableView: NSTableView): Long = files.size.toLong()

    @OptIn(ExperimentalForeignApi::class)
    override fun tableView(
        tableView: NSTableView,
        viewForTableColumn: NSTableColumn?,
        row: Long,
    ): NSView? {
        val cellView = tableView.makeViewWithIdentifier("FileTableCellView", owner = this) as? FileTableCellView
            ?: FileTableCellView(
                frame = NSMakeRect(
                    0.0,
                    0.0,
                    tableView.bounds.useContents { this.size.width },
                    100.0,
                ),
            ).apply {
                identifier = "FileTableCellView"
            }

        val item = files[row.toInt()]

        cellView.textField?.stringValue = item.name

        cellView.imageView?.image = item.documentDirectory
            .listImages()
            .let { images ->
                images.getOrNull(images.size / 2) // リストの真ん中の要素を取得
            }?.let { imagePath ->
                val url = NSURL.fileURLWithPath(imagePath.toString())
                NSImage(contentsOfURL = url)
            }

        return cellView
    }

    override fun tableViewSelectionDidChange(notification: NSNotification) {
        val tableView = (notification.`object` as? NSTableView) ?: return
        val selectedRow = tableView.selectedRow
        if (selectedRow != -1L) {
            val selectedFile = files[selectedRow.toInt()]
            parent.setDocument(selectedFile.documentDirectory)
        }
    }

    override fun tableView(
        tableView: NSTableView,
        heightOfRow: Long,
    ): Double = 50.0
}
