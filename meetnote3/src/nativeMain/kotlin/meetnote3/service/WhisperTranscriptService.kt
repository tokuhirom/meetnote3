package meetnote3.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.utils.ProcessBuilder
import meetnote3.utils.getHomeDirectory
import okio.FileSystem
import okio.Path

import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class WhisperTranscriptService(
    private val modelName: String = "medium",
    private val language: String = "japanese",
    private val whisperCppTimeout: Duration = Duration.parse("1h"),
    private val ffmpegTimeout: Duration = Duration.parse("1h"),
) {
    private val mutableTranscriptionCompletionFlow = MutableSharedFlow<DocumentDirectory>()
    val readyForSummarizeFlow: SharedFlow<DocumentDirectory> = mutableTranscriptionCompletionFlow.asSharedFlow()

    private val client = HttpClient(Darwin) {
    }

    suspend fun transcribe(documentDirectory: DocumentDirectory) {
        val mixedFilePath = documentDirectory.mixedFilePath()

        // Convert to wave file
        val waveFilePath = documentDirectory.waveFilePath()
        convertToWave(mixedFilePath, waveFilePath)

        try {
            // Download model
            val modelFileName = downloadModel(modelName)

            // Run whisper-cpp
            runWhisperCpp(modelFileName, waveFilePath, documentDirectory)

            mutableTranscriptionCompletionFlow.emit(documentDirectory)
        } finally {
            info("Clean up temporary wave file: file://$waveFilePath")
            FileSystem.SYSTEM.delete(waveFilePath)
        }
    }

    private suspend fun convertToWave(
        inputFilePath: Path,
        outputFilePath: Path,
    ) {
        val process =
            ProcessBuilder(
                "ffmpeg",
                "-i",
                inputFilePath.toString(),
                "-ar",
                // sample rate should be 16khz
                "16000",
                outputFilePath.toString(),
            ).start(captureStdout = true, captureStderr = true)
        val processExitStatus = process.waitUntil(ffmpegTimeout)
        if (!(processExitStatus.exited() && processExitStatus.exitstatus() == 0)) {
            throw Exception("ffmpeg failed with exit code $processExitStatus. Stderr: ${process.stderr?.slurpString()}")
        }
        if (FileSystem.SYSTEM.exists(outputFilePath)) {
            info("Converted to wave file: file://$outputFilePath")
        } else {
            throw Exception("Failed to convert to wave file: file://$outputFilePath")
        }
    }

    private suspend fun runWhisperCpp(
        modelFilePath: Path,
        waveFilePath: Path,
        documentDirectory: DocumentDirectory,
    ) {
        val outputLrcFilePath = documentDirectory.lrcFilePath()
        val process = ProcessBuilder(
            "whisper-cpp",
            "--model",
            modelFilePath.toString(),
            "--output-lrc",
            "--output-file",
            outputLrcFilePath.toString().replace(".lrc", ""),
            "--language",
            language,
            waveFilePath.toString(),
        ).start(captureStdout = false, captureStderr = false)
        val processExitStatus = process.waitUntil(whisperCppTimeout)
        if (!(processExitStatus.exited() && processExitStatus.termsig() == 0)) {
            throw Exception(
                "whisper-cpp failed with exit code $processExitStatus." +
                    " Stderr: ${process.stderr?.slurpString()}",
            )
        }

        if (FileSystem.SYSTEM.exists(outputLrcFilePath)) {
            info("Transcribed to lrc file: file://$outputLrcFilePath")
        } else {
            throw Exception("Failed to transcribe to lrc file: file://$outputLrcFilePath")
        }
    }

    private suspend fun downloadModel(modelName: String): Path {
        val baseUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"
        val modelUrl = "$baseUrl/ggml-$modelName.bin"

        val homedir = getHomeDirectory()
        val modelDirectory = homedir.resolve("Documents/models")
        FileSystem.SYSTEM.createDirectories(modelDirectory)

        val modelPath = modelDirectory.resolve("ggml-$modelName.bin")

        if (FileSystem.SYSTEM.exists(modelPath)) {
            return modelPath
        }

        info("Fetching model from $modelUrl to $modelPath")
        val response: HttpResponse = client.get(modelUrl)
        val responseBody = response.bodyAsChannel()

        withContext(Dispatchers.IO) {
            FileSystem.SYSTEM.write(modelPath) {
                val buffer = ByteArray(4096)
                while (!responseBody.isClosedForRead) {
                    val bytesRead = responseBody.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        this.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        return modelPath
    }
}
