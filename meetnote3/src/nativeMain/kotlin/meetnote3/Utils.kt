package meetnote3

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import platform.ScreenCaptureKit.SCShareableContent
import platform.posix.fprintf
import platform.posix.stderr
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
