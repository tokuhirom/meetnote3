package meetnote3.service

import meetnote3.utils.ProcessBuilder

import kotlin.time.Duration
import kotlinx.coroutines.runBlocking

class EnvironmentDiagnosticService {
    fun show() {
        runBlocking {
            ProcessBuilder("which", "python3")
                .start(captureStdout = false, captureStderr = false)
                .waitUntil(Duration.parse("10s"))

            ProcessBuilder("python3", "--version")
                .start(captureStdout = false, captureStderr = false)
                .waitUntil(Duration.parse("10s"))
        }
    }
}
