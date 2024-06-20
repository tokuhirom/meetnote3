package meetnote3

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import platform.Foundation.NSTemporaryDirectory
import platform.ScreenCaptureKit.SCShareableContent
import platform.posix.fprintf
import platform.posix.stderr
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

fun createTempFile(
    prefix: String,
    suffix: String,
): String {
    val tempDir = NSTemporaryDirectory()
    val fileName = "$prefix${Random.nextInt()}$suffix"
    val filePath = tempDir + fileName
    return filePath
}

@OptIn(ExperimentalForeignApi::class)
fun eprintln(message: String) {
    // print to stderr
    fprintf(stderr, "%s\n", message)
}

@BetaInteropApi
suspend fun getSharableContent(): SCShareableContent =
    suspendCoroutine { cont ->
        autoreleasepool {
            SCShareableContent.getShareableContentWithCompletionHandler { content: SCShareableContent?, error ->
                if (error != null) {
                    cont.resumeWithException(Exception("Error in getShareableContentWithCompletionHandler: ${error.localizedDescription}"))
                }
                if (content == null) {
                    cont.resumeWithException(NullPointerException("No content found."))
                }

                cont.resume(content!!)
            }
        }
    }
