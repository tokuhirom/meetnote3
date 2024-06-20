package meetnote3.service

import kotlinx.cinterop.BetaInteropApi
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
import platform.posix.perror
import platform.posix.unlink

class CaptureMixService {
    @BetaInteropApi
    suspend fun start(
        micFileName: String,
        screenFileName: String,
        outFileName: String,
    ): CaptureState {
        println("Recording audio and screen to $micFileName and $screenFileName ...")

        val micRecorder = startAudioRecording(AVFileTypeMPEG4, micFileName)
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
            screenFileName,
            contentFilter,
            enableVideo = false,
            enableAudio = true,
            captureConfiguration,
        )

        return CaptureState(
            micRecorder = micRecorder,
            screenRecorder = screenRecorder,
            outFileName = outFileName,
            micFile = micFileName,
            screenFile = screenFileName,
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
    suspend fun stop() {
        micRecorder.stop()

        screenRecorder.stop()
        println("Writing finished")

        println("Starting mix...")

        mix(inputFileNames = listOf(micFile, screenFile), outputFileName = outFileName)

        println("Created mix file: $outFileName")

        if (unlink(micFile) != 0) {
            perror("unlink $micFile")
            info("Failed to delete $micFile")
        }
        if (unlink(screenFile) != 0) {
            perror("unlink $screenFile")
            info("Failed to delete $screenFile")
        }
        info("Deleted temp files($micFile, $screenFile)")
    }
}
