package meetnote3.recorder

import platform.ScreenCaptureKit.SCDisplay
import platform.ScreenCaptureKit.SCShareableContent

fun findDefaultDisplay(displayCallback: (SCDisplay, List<*>) -> Unit) {
    SCShareableContent.getShareableContentWithCompletionHandler { content, error ->
        if (error != null) {
            println("Error getting shareable content: ${error.localizedDescription}")
            return@getShareableContentWithCompletionHandler
        }

        val display: SCDisplay? = content?.displays?.firstOrNull() as? SCDisplay
        if (display == null) {
            println("No display found")
            return@getShareableContentWithCompletionHandler
        }

        displayCallback(display, content.applications)
    }
}
