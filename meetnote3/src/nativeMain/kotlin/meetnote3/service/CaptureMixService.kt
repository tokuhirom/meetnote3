package meetnote3.service

import meetnote3.getSharableContent
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.recorder.MicRecorder
import meetnote3.recorder.ScreenRecorder
import meetnote3.recorder.mix
import meetnote3.recorder.startAudioRecording
import meetnote3.recorder.startImageCaptureJob
import meetnote3.recorder.startScreenAudioRecord
import okio.FileSystem
import platform.AVFoundation.AVFileTypeMPEG4
import platform.ScreenCaptureKit.SCContentFilter
import platform.ScreenCaptureKit.SCDisplay
import platform.ScreenCaptureKit.SCStreamConfiguration
import platform.ScreenCaptureKit.SCWindow
import platform.posix.getenv

import kotlin.time.Duration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Job

class CaptureMixService {
    @BetaInteropApi
    suspend fun start(documentDirectory: DocumentDirectory): CaptureState {
        val micFileName = documentDirectory.micFilePath().toString()
        val screenFileName = documentDirectory.screenFilePath()

        println("Recording audio and screen to $micFileName and $screenFileName ...")

        val micRecorder = startAudioRecording(AVFileTypeMPEG4, micFileName)
        println("Started micRecorder...")

        val content = getSharableContent()
        val display = (content.displays.firstOrNull() ?: error("No display found")) as SCDisplay

        println("Display found: $display")
        val targetWindow = content.windows.firstOrNull {
            if (it is SCWindow) {
                it.title == "Zoom Meeting" || it.title == "Zoom Webinar"
            } else {
                false
            }
        }

        val contentFilter = SCContentFilter(
            display,
            excludingWindows = emptyList<Any>(),
        )
        val captureConfiguration = SCStreamConfiguration().apply {
            capturesAudio = true
        }

        val screenAudioRecorder = startScreenAudioRecord(
            screenFileName.toString(),
            contentFilter,
            enableAudio = true,
            captureConfiguration,
        )

        val imageRecorderJob = if (targetWindow != null) {
            info("Image is ready to record: $targetWindow")
            // When simplifying the implementation by launching multiple instances of ScreenCaptureKit, I
            // encountered an issue where only one instance was receiving the data. It is important to be aware
            // of this limitation.
            startImageCaptureJob(
                (targetWindow as SCWindow).windowID,
                documentDirectory,
                interval = Duration.parse("1m"),
            )
        } else {
            info("Target window is missing. Skip image recording.")
            null
        }

        return CaptureState(
            documentDirectory = documentDirectory,
            micRecorder = micRecorder,
            screenAudioRecorder = screenAudioRecorder,
            imageRecorderJob = imageRecorderJob,
        )
    }
}

data class CaptureState(
    val documentDirectory: DocumentDirectory,
    private val micRecorder: MicRecorder,
    private val screenAudioRecorder: ScreenRecorder,
    private val imageRecorderJob: Job?,
) {
    @OptIn(ExperimentalForeignApi::class)
    suspend fun stop() {
        val micFile = documentDirectory.micFilePath()
        val screenFile = documentDirectory.screenFilePath()
        val outFileName = documentDirectory.mixedFilePath()

        micRecorder.stop()

        try {
            screenAudioRecorder.stop()
        } catch (e: Exception) {
            info("Failed to stop screenAudioRecorder: $e")
        }
        try {
            imageRecorderJob?.cancel()
        } catch (e: Exception) {
            info("Failed to stop imageRecorder: $e")
        }
        println("Writing finished")

        println("Starting mix...")

        mix(
            inputFileNames = listOf(micFile.toString(), screenFile.toString()),
            outputFileName = outFileName.toString(),
        )

        println("Created mix file: $outFileName")

        // これを外すと、なぜかスピーカー音声がうまくミックスされない気がする。
        if (getenv("MEETNOTE3_KEEP_TEMP_FILES") == null) {
            FileSystem.SYSTEM.delete(micFile, false)
            FileSystem.SYSTEM.delete(screenFile, false)
            info("Deleted temp files($micFile, $screenFile)")
        } else {
            info("Keep temp files due to MEETNOTE3_KEEP_TEMP_FILES($micFile, $screenFile)")
        }
    }
}
