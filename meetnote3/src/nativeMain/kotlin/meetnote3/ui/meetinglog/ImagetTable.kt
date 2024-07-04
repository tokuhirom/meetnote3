package meetnote3.ui.meetinglog

import okio.Path
import platform.AppKit.NSImage
import platform.AppKit.NSImageScaleProportionallyUpOrDown
import platform.AppKit.NSImageView
import platform.AppKit.NSTableCellView
import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableView
import platform.AppKit.NSTableViewDataSourceProtocol
import platform.AppKit.NSTableViewDelegateProtocol
import platform.AppKit.NSView
import platform.CoreGraphics.CGRect
import platform.Foundation.NSMakeRect
import platform.Foundation.NSURL
import platform.darwin.NSObject

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

data class ImageTableItem(
    val path: Path,
)

class ImageTableViewDelegate :
    NSObject(),
    NSTableViewDelegateProtocol,
    NSTableViewDataSourceProtocol {
    private val images = mutableListOf<ImageTableItem>()

    fun updateImages(newImages: List<ImageTableItem>) {
        images.clear()
        images.addAll(newImages)
    }

    override fun numberOfRowsInTableView(tableView: NSTableView): Long = images.size.toLong()

    @OptIn(ExperimentalForeignApi::class)
    override fun tableView(
        tableView: NSTableView,
        viewForTableColumn: NSTableColumn?,
        row: Long,
    ): NSView? {
        val cellView =
            tableView.makeViewWithIdentifier("ImageTableCellView", owner = this) as? ImageTableCellView
                ?: ImageTableCellView(
                    frame = NSMakeRect(
                        0.0,
                        0.0,
                        tableView.bounds.useContents { this.size.width },
                        100.0,
                    ),
                ).apply {
                    identifier = "ImageTableCellView"
                }

        val item = images[row.toInt()]

        val url = NSURL.fileURLWithPath(item.path.toString())
        val nsImage = NSImage(contentsOfURL = url)
        cellView.imageView?.image = nsImage

        return cellView
    }

    override fun tableView(
        tableView: NSTableView,
        heightOfRow: Long,
    ): Double = 100.0
}

@OptIn(ExperimentalForeignApi::class)
class ImageTableCellView
    @OptIn(ExperimentalForeignApi::class)
    constructor(
        frame: CValue<CGRect>,
    ) : NSTableCellView(frame) {
        init {
            imageView = NSImageView().apply {
                setFrame(NSMakeRect(0.0, 0.0, 300.0, 100.0))
                imageScaling = NSImageScaleProportionallyUpOrDown
            }

            addSubview(imageView!!)
        }
    }
