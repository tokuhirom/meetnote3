package meetnote3.ui

import meetnote3.model.DocumentDirectory
import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableView
import platform.AppKit.NSTableViewDataSourceProtocol
import platform.AppKit.NSTableViewDelegateProtocol
import platform.Foundation.NSNotification
import platform.darwin.NSObject

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

    override fun tableView(
        tableView: NSTableView,
        objectValueForTableColumn: NSTableColumn?,
        row: Long,
    ): Any? = files[row.toInt()].name

    override fun tableViewSelectionDidChange(notification: NSNotification) {
        val tableView = (notification.`object` as? NSTableView) ?: return
        val selectedRow = tableView.selectedRow
        if (selectedRow != -1L) {
            val selectedFile = files[selectedRow.toInt()]
            parent.setDocument(selectedFile.documentDirectory)
        }
    }
}
