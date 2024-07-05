package meetnote3.ui.meetinglog

import meetnote3.transcript.LrcLine
import platform.AppKit.NSTableCellView
import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableView
import platform.AppKit.NSTableViewDataSourceProtocol
import platform.AppKit.NSTableViewDelegateProtocol
import platform.AppKit.NSTextField
import platform.AppKit.NSView
import platform.darwin.NSObject

class LrcTableViewDelegate :
    NSObject(),
    NSTableViewDelegateProtocol,
    NSTableViewDataSourceProtocol {
    private var lrcItems: List<LrcLine> = listOf()

    fun updateLrcItems(newItems: List<LrcLine>) {
        lrcItems = newItems
    }

    override fun numberOfRowsInTableView(tableView: NSTableView): Long = lrcItems.size.toLong()

    override fun tableView(
        tableView: NSTableView,
        viewFor: NSTableColumn?,
        row: Long,
    ): NSView? {
        val item = lrcItems[row.toInt()]
        val identifier = viewFor?.identifier?.toString()

        val cell = tableView.makeViewWithIdentifier(identifier ?: "", owner = this) as? NSTableCellView
            ?: NSTableCellView().apply {
                setIdentifier(identifier)
                textField = NSTextField().apply {
                    setEditable(false)
                    drawsBackground = false
                    setBordered(false)
                }
                addSubview(textField!!)
            }

        when (identifier) {
            "Timestamp" -> cell.textField?.stringValue = item.timestamp
            "Content" -> cell.textField?.stringValue = item.content
        }

        return cell
    }

    override fun tableView(
        tableView: NSTableView,
        shouldEditTableColumn: NSTableColumn?,
        row: Long,
    ): Boolean = false
}
