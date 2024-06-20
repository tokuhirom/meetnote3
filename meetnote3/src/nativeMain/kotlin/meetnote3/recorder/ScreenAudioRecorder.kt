package meetnote3.recorder

import kotlinx.cinterop.ExperimentalForeignApi
import meetnote3.eprintln
import meetnote3.utils.fileExists
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVFileTypeAppleM4A
import platform.AVFoundation.AVFileTypeQuickTimeMovie
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoAverageBitRateKey
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeH264
import platform.AVFoundation.AVVideoCompressionPropertiesKey
import platform.AVFoundation.AVVideoHeightKey
import platform.AVFoundation.AVVideoProfileLevelH264HighAutoLevel
import platform.AVFoundation.AVVideoProfileLevelKey
import platform.AVFoundation.AVVideoWidthKey
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
import platform.posix.exit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
fun createAssetWriter(
    fileName: String,
    isVideo: Boolean,
): AVAssetWriter {
    val outputFileURL = NSURL.fileURLWithPath(fileName)
    println("Output file: ${outputFileURL.path}")

    val fileType = if (isVideo) AVFileTypeQuickTimeMovie else AVFileTypeAppleM4A
    val assetWriter = AVAssetWriter(outputFileURL, fileType = fileType, error = null)

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
fun createVideoWriterInput(): AVAssetWriterInput {
    val videoSettings = mapOf<Any?, Any?>(
        AVVideoCodecKey to AVVideoCodecTypeH264,
        AVVideoWidthKey to 1920,
        AVVideoHeightKey to 1080,
        AVVideoCompressionPropertiesKey to mapOf(
            AVVideoAverageBitRateKey to 6000000,
            AVVideoProfileLevelKey to AVVideoProfileLevelH264HighAutoLevel,
        ),
    )
    return AVAssetWriterInput(
        mediaType = AVMediaTypeVideo,
        outputSettings = videoSettings,
        sourceFormatHint = null,
    )
}

@OptIn(ExperimentalForeignApi::class)
suspend fun startScreenRecord(
    fileName: String,
    contentFilter: SCContentFilter,
    enableVideo: Boolean,
    enableAudio: Boolean,
    scStreamConfiguration: SCStreamConfiguration,
): ScreenRecorder {
    val stream = SCStream(contentFilter, scStreamConfiguration, null)

    val assetWriter = createAssetWriter(fileName, enableVideo)

    val audioWriterInput = if (enableAudio) {
        println("Adding audio input")
        val audioWriterInput = createAudioWriterInput()
        assetWriter.addInput(audioWriterInput)
        audioWriterInput
    } else {
        println("Not adding audio input")
        null
    }

    val videoWriterInput = if (enableVideo) {
        val videoInput = createVideoWriterInput()
        println("Adding video input")
        assetWriter.addInput(videoInput)
        videoInput
    } else {
        null
    }

    if (!assetWriter.startWriting()) {
        if (fileExists(fileName)) {
            eprintln("File already exists: $fileName")
            exit(1)
        }
        eprintln("Failed to start writing: ${assetWriter.error?.localizedDescription}")
        exit(1)
    }

    // CMClock.hostTimeClock.time
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
                eprintln("Invalid sample buffer")
                return
            }

            when (ofType) {
                SCStreamOutputType.SCStreamOutputTypeAudio -> {
                    if (audioWriterInput?.readyForMoreMediaData == true) {
                        if (!audioWriterInput.appendSampleBuffer(didOutputSampleBuffer!!)) {
                            println("Cannot write audio")
                        }
                    } else {
                        println("Audio writer input not ready for more media data")
                    }
                }

                SCStreamOutputType.SCStreamOutputTypeScreen -> {
                    if (videoWriterInput?.readyForMoreMediaData == true) {
                        if (!videoWriterInput.appendSampleBuffer(didOutputSampleBuffer!!)) {
                            println("Cannot write video")
                        }
                    } else {
                        println("Video writer input not ready for more media data")
                    }
                }
            }
        }
    }

    if (enableAudio) {
        stream.addStreamOutput(
            streamOutput,
            SCStreamOutputType.SCStreamOutputTypeAudio,
            sampleHandlerQueue = null,
            error = null,
        )
    }
    if (enableVideo) {
        stream.addStreamOutput(
            streamOutput,
            SCStreamOutputType.SCStreamOutputTypeScreen,
            sampleHandlerQueue = null,
            error = null,
        )
    }

    stream.startCapture()

    return ScreenRecorder(stream, audioWriterInput, videoWriterInput, assetWriter)
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
    val videoWriterInput: AVAssetWriterInput?,
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
        videoWriterInput?.markAsFinished()
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
