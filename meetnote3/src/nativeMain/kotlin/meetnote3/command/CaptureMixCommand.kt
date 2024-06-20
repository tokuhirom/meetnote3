package meetnote3.command

import meetnote3.createTempFile
import meetnote3.getSharableContent
import meetnote3.info
import meetnote3.recorder.MicRecorder
import meetnote3.recorder.ScreenRecorder
import meetnote3.recorder.mix
import meetnote3.recorder.startAudioRecording
import meetnote3.recorder.startScreenRecord
import platform.AVFoundation.AVFileTypeMPEG4
import platform.ScreenCaptureKit.SCContentFilter
import platform.ScreenCaptureKit.SCDisplay
import platform.ScreenCaptureKit.SCStreamConfiguration
import platform.posix.sleep
import platform.posix.unlink

class CaptureMixCommand(
    private val outFileName: String,
) {
    suspend fun run(): CaptureState {
        val micFile = createTempFile("capjoy-mix-mic-", ".m4a")
        val screenFile = createTempFile("capjoy-mix-screen-", ".m4a")

        println("Recording audio and screen to $micFile and $screenFile ...")

        val micRecorder = startAudioRecording(AVFileTypeMPEG4, micFile)
        println("Started micRecorder...")

        val content = getSharableContent()
        val display = (content.displays.firstOrNull() ?: error("No display found")) as SCDisplay

        println("Display found: $display")

        val contentFilter = SCContentFilter(
            display,
            excludingWindows = emptyList<Any>(),
        )
        val captureConfiguration = SCStreamConfiguration().apply {
            capturesAudio = true
        }

        val screenRecorder = startScreenRecord(
            screenFile,
            contentFilter,
            enableVideo = false,
            enableAudio = true,
            captureConfiguration,
        )

        sleep(10u)

        return CaptureState(
            micRecorder = micRecorder,
            screenRecorder = screenRecorder,
            outFileName = outFileName,
            micFile = micFile,
            screenFile = screenFile,
        )
    }
}

data class CaptureState(
    val micRecorder: MicRecorder,
    val screenRecorder: ScreenRecorder,
    val outFileName: String,
    val micFile: String,
    val screenFile: String,
) {
    fun stop() {
        micRecorder.stop()

        screenRecorder.stop {
            println("Writing finished")

            println("Starting mix...")

            mix(listOf(micFile, screenFile), outFileName)

            println("Created mix file: $outFileName")

            unlink(micFile)
            unlink(screenFile)
            info("Deleted temp files($micFile, $screenFile)")
        }
    }
}
