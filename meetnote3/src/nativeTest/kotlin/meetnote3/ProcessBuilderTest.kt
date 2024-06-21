package meetnote3

import meetnote3.utils.ProcessBuilder

import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking

class ProcessBuilderTest {
    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun test() {
        runBlocking {
            val status = ProcessBuilder("ls", "-l")
                .start(captureStdout = false, captureStderr = false)
                .waitUntil(Duration.parse("10s"))
            assert(status == 0)
        }
    }
}
