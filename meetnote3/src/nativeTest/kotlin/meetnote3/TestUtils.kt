package meetnote3

import platform.posix.getenv

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun runOnLocalOnly(function: () -> Unit) {
    if (getenv("CI") == null) {
        function()
    } else {
        println("Skipping test on CI")
    }
}
