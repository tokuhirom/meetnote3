package meetnote3.ui.meetinglog

import meetnote3.model.DocumentDirectory
import okio.FileSystem
import okio.IOException
import platform.AppKit.NSImage
import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableView
import platform.AppKit.NSTableViewDataSourceProtocol
import platform.AppKit.NSTableViewDelegateProtocol
import platform.AppKit.NSView
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
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

        // タイトルを設定
        cellView.textField?.stringValue = item.name

        // 画像を設定（非同期で読み込み）
        val images = item.documentDirectory.listImages()
        if (images.isNotEmpty()) {
            val imagePath = images.first()
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)) {
                val imageData = try {
                    FileSystem.SYSTEM.read(imagePath) {
                        readByteArray()
                    }
                } catch (e: IOException) {
                    null
                }

                if (imageData != null) {
                    val nsImage = NSImage(data = imageData.toNSData())
                    dispatch_sync(dispatch_get_main_queue()) {
                        cellView.imageView?.image = nsImage
                    }
                } else {
                    dispatch_sync(dispatch_get_main_queue()) {
                        cellView.imageView?.image = null // 画像がない場合は空
                    }
                }
            }
        } else {
            cellView.imageView?.image = null // 画像がない場合
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
