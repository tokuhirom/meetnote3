package meetnote3.recorder

import meetnote3.eprintln
import meetnote3.info
import meetnote3.model.DocumentDirectory
import okio.Path
import platform.AppKit.NSBitmapImageFileType
import platform.AppKit.NSBitmapImageRep
import platform.AppKit.NSCIImageRep
import platform.AppKit.NSImage
import platform.AppKit.representationUsingType
import platform.CoreImage.CIImage
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferIsValid
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVImageBufferRef
import platform.Foundation.writeToFile
import platform.ScreenCaptureKit.SCContentFilter
import platform.ScreenCaptureKit.SCStream
import platform.ScreenCaptureKit.SCStreamConfiguration
import platform.ScreenCaptureKit.SCStreamOutputProtocol
import platform.ScreenCaptureKit.SCStreamOutputType
import platform.darwin.NSObject

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
suspend fun startScreenImageRecord(
    contentFilter: SCContentFilter,
    scStreamConfiguration: SCStreamConfiguration,
    documentDirectory: DocumentDirectory,
): ScreenImageRecorder {
    val stream = SCStream(contentFilter, scStreamConfiguration, null)

    val streamOutput = object : NSObject(), SCStreamOutputProtocol {
        override fun stream(
            stream: SCStream,
            didOutputSampleBuffer: CMSampleBufferRef?,
            ofType: SCStreamOutputType,
        ) {
            if (!CMSampleBufferIsValid(didOutputSampleBuffer)) {
                eprintln("Invalid sample buffer")
                return
            }

            if (ofType == SCStreamOutputType.SCStreamOutputTypeScreen) {
                val imageBuffer: CVImageBufferRef = CMSampleBufferGetImageBuffer(didOutputSampleBuffer)
                    ?: return
                val ciImage = CIImage(cVImageBuffer = imageBuffer)
                val rep = NSCIImageRep(ciImage)
                val nsImage = NSImage(size = rep.size)
                nsImage.addRepresentation(rep)
                val imageFileName = documentDirectory.createImageFileName()
                saveImageToFile(
                    nsImage,
                    imageFileName,
                    NSBitmapImageFileType.NSBitmapImageFileTypePNG,
                )
            }
        }
    }

    stream.addStreamOutput(
        streamOutput,
        SCStreamOutputType.SCStreamOutputTypeScreen,
        sampleHandlerQueue = null,
        error = null,
    )

    stream.startCapture()

    return ScreenImageRecorder(stream)
}

private fun saveImageToFile(
    image: NSImage,
    filePath: Path,
    fileFormat: NSBitmapImageFileType,
): Boolean {
    val imageData = image.TIFFRepresentation ?: return false
    val bitmapImageRep = NSBitmapImageRep(data = imageData) ?: return false
    val pngData = bitmapImageRep.representationUsingType(
        fileFormat,
        properties = emptyMap<Any?, Any>(),
    )
    info("Writing image to $filePath")
    return pngData?.writeToFile(filePath.toString(), atomically = true) ?: false
}

data class ScreenImageRecorder(
    val stream: SCStream,
) {
    suspend fun stop() {
        stream.stopCapture()
        println("Capture stopped")
    }
}
