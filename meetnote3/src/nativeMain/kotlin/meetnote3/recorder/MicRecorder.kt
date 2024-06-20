package meetnote3.recorder

import platform.AVFoundation.AVCaptureAudioFileOutput
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureFileOutput
import platform.AVFoundation.AVCaptureFileOutputRecordingDelegateProtocol
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVFileType
import platform.AVFoundation.AVMediaTypeAudio
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun startAudioRecording(
    outFormat: AVFileType,
    fileName: String,
): MicRecorder {
    val captureSession = AVCaptureSession()
    val audioDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio)
    val audioInput = AVCaptureDeviceInput.deviceInputWithDevice(audioDevice!!, null)

    if (captureSession.canAddInput(audioInput!!)) {
        captureSession.addInput(audioInput)
    } else {
        error("Failed to add audio input")
    }

    val outputFileURL = NSURL.fileURLWithPath(fileName)
    val audioOutput = AVCaptureAudioFileOutput()

    if (captureSession.canAddOutput(audioOutput)) {
        captureSession.addOutput(audioOutput)
    } else {
        error("Failed to add audio output")
    }

    captureSession.startRunning()

    val delegate =
        object : NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {
            override fun captureOutput(
                captureOutput: AVCaptureFileOutput,
                didFinishRecordingToOutputFileAtURL: NSURL,
                fromConnections: List<*>,
                error: NSError?,
            ) {
                println("Recording finished")
            }

            override fun captureOutput(
                output: AVCaptureFileOutput,
                didPauseRecordingToOutputFileAtURL: NSURL,
                fromConnections: List<*>,
            ) {
                println("Recording started $didPauseRecordingToOutputFileAtURL")
            }
        }

    audioOutput.startRecordingToOutputFileURL(outputFileURL, outFormat, delegate)

    return MicRecorder(audioOutput, captureSession)
}

data class MicRecorder(
    val audioOutput: AVCaptureAudioFileOutput,
    val captureSession: AVCaptureSession,
) {
    fun stop() {
        audioOutput.stopRecording()
        captureSession.stopRunning()
    }
}
