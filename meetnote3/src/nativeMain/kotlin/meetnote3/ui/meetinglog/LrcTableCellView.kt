package meetnote3.ui.meetinglog

import platform.AppKit.NSTableCellView
import platform.AppKit.NSTextField
import platform.CoreGraphics.CGRect
import platform.Foundation.NSMakeRect

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class LrcTableCellView(
    frame: CValue<CGRect>,
) : NSTableCellView(frame) {
    init {
        textField = NSTextField().apply {
            setFrame(NSMakeRect(0.0, 0.0, 630.0, 20.0))
            setEditable(false)
            setBezeled(false)
            drawsBackground = false
        }

        addSubview(textField!!)
    }
}
