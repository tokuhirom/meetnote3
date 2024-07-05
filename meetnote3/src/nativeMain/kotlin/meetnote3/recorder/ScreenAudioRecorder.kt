package meetnote3.recorder

import meetnote3.debug
import meetnote3.info
import meetnote3.warn
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVFileTypeAppleM4A
import platform.AVFoundation.AVMediaTypeAudio
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.CoreMedia.CMClockGetHostTimeClock
import platform.CoreMedia.CMClockGetTime
import platform.CoreMedia.CMSampleBufferIsValid
import platform.CoreMedia.CMSampleBufferRef
import platform.Foundation.NSURL
import platform.ScreenCaptureKit.SCContentFilter
import platform.ScreenCaptureKit.SCStream
import platform.ScreenCaptureKit.SCStreamConfiguration
import platform.ScreenCaptureKit.SCStreamOutputProtocol
import platform.ScreenCaptureKit.SCStreamOutputType
import platform.darwin.NSObject

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun createAssetWriter(fileName: String): AVAssetWriter {
    val outputFileURL = NSURL.fileURLWithPath(fileName)
    println("Output file: ${outputFileURL.path}")

    val assetWriter = AVAssetWriter(outputFileURL, fileType = AVFileTypeAppleM4A, error = null)

    return assetWriter
}

@OptIn(ExperimentalForeignApi::class)
fun createAudioWriterInput(): AVAssetWriterInput {
    val audioSettings = mapOf<Any?, Any?>(
        AVFormatIDKey to kAudioFormatMPEG4AAC,
        AVNumberOfChannelsKey to 1,
        AVSampleRateKey to 44100.0,
        AVEncoderBitRateKey to 64000,
    )
    return AVAssetWriterInput(
        mediaType = AVMediaTypeAudio,
        outputSettings = audioSettings,
        sourceFormatHint = null,
    )
}

@OptIn(ExperimentalForeignApi::class)
suspend fun startScreenAudioRecord(
    fileName: String,
    contentFilter: SCContentFilter,
    scStreamConfiguration: SCStreamConfiguration,
): ScreenRecorder {
    val stream = SCStream(contentFilter, scStreamConfiguration, null)

    val assetWriter = createAssetWriter(fileName)

    val audioWriterInput = run {
        println("Adding audio input")
        val audioWriterInput = createAudioWriterInput()
        assetWriter.addInput(audioWriterInput)
        audioWriterInput
    }

    if (!assetWriter.startWriting()) {
        if (FileSystem.SYSTEM.exists(fileName.toPath())) {
            error("File already exists: $fileName")
        }
        error("Failed to start writing: ${assetWriter.error?.localizedDescription}")
    }

    val hostTimeClock = CMClockGetHostTimeClock()
    val now = CMClockGetTime(hostTimeClock)
    assetWriter.startSessionAtSourceTime(now)

    val streamOutput = object : NSObject(), SCStreamOutputProtocol {
        override fun stream(
            stream: SCStream,
            didOutputSampleBuffer: CMSampleBufferRef?,
            ofType: SCStreamOutputType,
        ) {
            if (!CMSampleBufferIsValid(didOutputSampleBuffer)) {
                error("Invalid sample buffer")
                return
            }

            if (audioWriterInput.readyForMoreMediaData) {
                if (!audioWriterInput.appendSampleBuffer(didOutputSampleBuffer!!)) {
                    warn("Cannot write audio")
                } else {
                    debug("Audio written")
                }
            } else {
                info("Audio writer input not ready for more media data")
            }
        }
    }

    stream.addStreamOutput(
        streamOutput,
        SCStreamOutputType.SCStreamOutputTypeAudio,
        sampleHandlerQueue = null,
        error = null,
    )

    stream.startCapture()

    return ScreenRecorder(stream, audioWriterInput, assetWriter)
}

suspend fun SCStream.startCapture() =
    suspendCoroutine { cont ->
        this.startCaptureWithCompletionHandler { error ->
            if (error != null) {
                cont.resumeWithException(Exception("Failed to start capture: ${error.localizedDescription}"))
            } else {
                cont.resume(Unit)
            }
        }
    }

data class ScreenRecorder(
    val stream: SCStream,
    val audioWriterInput: AVAssetWriterInput?,
    val assetWriter: AVAssetWriter,
) {
    @OptIn(ExperimentalForeignApi::class)
    suspend fun stop() {
        stream.stopCapture()

        println("Capture stopped")

        val hostTimeClock = CMClockGetHostTimeClock()
        val now = CMClockGetTime(hostTimeClock)
        assetWriter.endSessionAtSourceTime(now)

        audioWriterInput?.markAsFinished()
        assetWriter.finishWritingAsync()
    }
}

suspend fun SCStream.stopCapture() =
    suspendCoroutine { cont ->
        this.stopCaptureWithCompletionHandler { error ->
            if (error != null) {
                cont.resumeWithException(Exception("Failed to stop capture: ${error.localizedDescription}"))
            } else {
                cont.resume(Unit)
            }
        }
    }

suspend fun AVAssetWriter.finishWritingAsync() =
    suspendCoroutine { cont ->
        this.finishWritingWithCompletionHandler {
            cont.resume(Unit)
        }
    }
