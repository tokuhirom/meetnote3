package meetnote3.recorder

import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.warn
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFURLCreateWithFileSystemPath
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFURLPOSIXPathStyle
import platform.CoreGraphics.CGRectNull
import platform.CoreGraphics.CGWindowID
import platform.CoreGraphics.CGWindowListCreateImage
import platform.CoreGraphics.kCGWindowImageDefault
import platform.CoreGraphics.kCGWindowListOptionIncludingWindow
import platform.CoreServices.kUTTypePNG
import platform.ImageIO.CGImageDestinationAddImage
import platform.ImageIO.CGImageDestinationCreateWithURL
import platform.ImageIO.CGImageDestinationFinalize

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun startImageCaptureJob(
    windowID: CGWindowID,
    documentDirectory: DocumentDirectory,
    interval: Duration,
): CompletableJob {
    val job = Job()
    val scope = CoroutineScope(Dispatchers.Default)
    scope.launch {
        runImageCaptureLoop(windowID, documentDirectory, interval, job)
    }
    return job
}

@OptIn(ExperimentalForeignApi::class)
suspend fun runImageCaptureLoop(
    windowID: CGWindowID,
    documentDirectory: DocumentDirectory,
    interval: Duration,
    job: Job,
) {
    withContext(Dispatchers.Default + job) {
        try {
            while (this.isActive) {
                val imageFileName = documentDirectory.createImageFileName()
                captureWindow(
                    windowID = windowID,
                    filePath = imageFileName.toString(),
                    imageFormat = kUTTypePNG,
                )
                delay(interval)
            }
        } catch (e: CancellationException) {
            println("Image capture loop cancelled")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun captureWindow(
    windowID: CGWindowID,
    filePath: String,
    imageFormat: CFStringRef?,
) {
    val image =
        CGWindowListCreateImage(
            CGRectNull.readValue(),
            kCGWindowListOptionIncludingWindow,
            windowID,
            kCGWindowImageDefault,
        )

    if (image != null) {
        val filePathCFString =
            CFStringCreateWithCString(kCFAllocatorDefault, filePath, kCFStringEncodingUTF8)
        val url =
            CFURLCreateWithFileSystemPath(
                kCFAllocatorDefault,
                filePathCFString,
                kCFURLPOSIXPathStyle,
                false,
            )

        val destination = CGImageDestinationCreateWithURL(url, imageFormat, 1.convert(), null)
        if (destination != null) {
            CGImageDestinationAddImage(destination, image, null)
            if (CGImageDestinationFinalize(destination)) {
                info("Image saved to $filePath")
            } else {
                warn("Failed to finalize image destination")
            }
        } else {
            warn("Failed to create image destination")
        }
    } else {
        warn("Failed to create image")
    }
}
