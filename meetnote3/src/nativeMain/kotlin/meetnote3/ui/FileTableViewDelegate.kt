package meetnote3.ui

import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableView
import platform.AppKit.NSTableViewDataSourceProtocol
import platform.AppKit.NSTableViewDelegateProtocol
import platform.Foundation.NSNotification
import platform.darwin.NSObject

class FileTableViewDelegate(
    private val parent: MeetingLogDialog,
) : NSObject(),
    NSTableViewDelegateProtocol,
    NSTableViewDataSourceProtocol {
    private val files = mutableListOf<String>()

    fun updateFiles(newFiles: List<String>) {
        files.clear()
        files.addAll(newFiles)
    }

    override fun numberOfRowsInTableView(tableView: NSTableView): Long = files.size.toLong()

    override fun tableView(
        tableView: NSTableView,
        objectValueForTableColumn: NSTableColumn?,
        row: Long,
    ): Any? = files[row.toInt()]

    override fun tableViewSelectionDidChange(notification: NSNotification) {
        val tableView = (notification.`object` as? NSTableView) ?: return
        val selectedRow = tableView.selectedRow
        if (selectedRow != -1L) {
            val selectedFile = files[selectedRow.toInt()]
            parent.setDocument(selectedFile)
        }
    }
}
