package meetnote3.transcript

import meetnote3.info
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Speech.SFSpeechRecognitionResult
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognitionTaskDelegateProtocol
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerDelegateProtocol
import platform.Speech.SFSpeechURLRecognitionRequest
import platform.Speech.SFTranscription
import platform.Speech.SFTranscriptionSegment
import platform.darwin.NSObject

fun transcribeAudio(filePath: String) {
    info("Loading audio file from $filePath")
    val url = NSURL.fileURLWithPath(filePath)
    val request = SFSpeechURLRecognitionRequest(url)
//    request.requiresOnDeviceRecognition = true

    val locale = NSLocale(localeIdentifier = "ja-JP")
    val recognizer = SFSpeechRecognizer(locale)
    if (!recognizer.available) {
        error("Recognizer is not available")
    }
    recognizer.delegate = object : NSObject(), SFSpeechRecognizerDelegateProtocol {
        override fun speechRecognizer(
            speechRecognizer: SFSpeechRecognizer,
            availabilityDidChange: Boolean,
        ) {
            info("Recognizer availability changed: $availabilityDidChange")
        }
    }
    info("Recognizer status: ${recognizer.available}")
    info("supportsOnDeviceRecognition: ${recognizer.supportsOnDeviceRecognition}")
    recognizer.recognitionTaskWithRequest(
        request,
        object : NSObject(), SFSpeechRecognitionTaskDelegateProtocol {
            override fun speechRecognitionDidDetectSpeech(task: SFSpeechRecognitionTask) {
                info("[TRR] Detected speech")
            }

            override fun speechRecognitionTask(
                task: SFSpeechRecognitionTask,
                didFinishRecognition: SFSpeechRecognitionResult,
            ) {
                // if the audio file doesn't contain any speech, didFinishRecognition will be false.
                info("[TRR] Finished recognition: $didFinishRecognition")
            }

            override fun speechRecognitionTask(
                task: SFSpeechRecognitionTask,
                didFinishSuccessfully: Boolean,
            ) {
                info("[TRR] Finished successfully: $didFinishSuccessfully")
            }

            override fun speechRecognitionTaskWasCancelled(task: SFSpeechRecognitionTask) {
                info("[TRR] Task was cancelled")
            }

            override fun speechRecognitionTask(
                task: SFSpeechRecognitionTask,
                didHypothesizeTranscription: SFTranscription,
            ) {
                didHypothesizeTranscription.segments.lastOrNull()?.let {
                    if (it is SFTranscriptionSegment) {
                        info("IOOP ${it.timestamp} ${it.substring}")
                    }
                }
            }

            override fun speechRecognitionTaskFinishedReadingAudio(task: SFSpeechRecognitionTask) {
                info("[TRR] Finished reading audio")
            }
        },
    )

    info("Transcription started")
}

fun processFile() {
    val inputFile = "/tmp/input.m4a"

    info("Starting transcription of $inputFile")
    transcribeAudio(inputFile)
}
