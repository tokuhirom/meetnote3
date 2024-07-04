package meetnote3.ui.meetinglog

import platform.AppKit.NSImageScaleProportionallyUpOrDown
import platform.AppKit.NSImageView
import platform.AppKit.NSTableCellView
import platform.AppKit.NSTextField
import platform.CoreGraphics.CGRect
import platform.Foundation.NSMakeRect

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class FileTableCellView(
    frame: CValue<CGRect>,
) : NSTableCellView(frame) {
    init {
        imageView = NSImageView().apply {
            setFrame(NSMakeRect(0.0, 0.0, 50.0, 50.0))
            imageScaling = NSImageScaleProportionallyUpOrDown
        }

        textField = NSTextField().apply {
            setFrame(NSMakeRect(60.0, 0.0, 200.0, 50.0))
            setEditable(false)
            setBezeled(false)
            drawsBackground = false
        }

        addSubview(imageView!!)
        addSubview(textField!!)
    }
}
