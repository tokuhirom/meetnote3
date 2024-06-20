package meetnote3.service

import meetnote3.getSharableContent
import meetnote3.info
import meetnote3.model.DocumentDirectory
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

import kotlinx.cinterop.BetaInteropApi

class CaptureMixService {
    @BetaInteropApi
    suspend fun start(documentDirectory: DocumentDirectory): CaptureState {
        val micFileName = documentDirectory.micFilePath()
        val screenFileName = documentDirectory.screenFilePath()

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
            documentDirectory = documentDirectory,
            micRecorder = micRecorder,
            screenRecorder = screenRecorder,
        )
    }
}

data class CaptureState(
    val documentDirectory: DocumentDirectory,
    private val micRecorder: MicRecorder,
    private val screenRecorder: ScreenRecorder,
) {
    suspend fun stop() {
        val micFile = documentDirectory.micFilePath()
        val screenFile = documentDirectory.screenFilePath()
        val outFileName = documentDirectory.mixedFilePath()

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
