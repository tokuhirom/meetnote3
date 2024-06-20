package meetnote3

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
fun runOnLocalOnly(function: () -> Unit) {
    if (getenv("CI") == null) {
        function()
    } else {
        println("Skipping test on CI")
    }
}
