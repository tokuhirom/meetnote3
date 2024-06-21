package meetnote3.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.utils.ProcessBuilder
import meetnote3.utils.fileExists
import meetnote3.utils.getHomeDirectory
import meetnote3.utils.mkdirP
import okio.FileSystem
import okio.Path
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class WhisperTranscriptService(
    private val modelName: String = "small",
    private val language: String = "japanese",
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
                "16000", // sample rate should be 16khz
                outputFilePath.toString(),
            ).start(captureStdout = true, captureStderr = true)
        val exitCode = process.waitUntil(kotlin.time.Duration.parse("60s"))
        if (exitCode != 0) {
            throw Exception("ffmpeg failed with exit code $exitCode. Stderr: ${process.stderr?.slurpString()}")
        }
        if (fileExists(outputFilePath.toString())) {
            info("Converted to wave file: file://$outputFilePath")
        } else {
            throw Exception("Failed to convert to wave file: file://$outputFilePath")
        }
    }

    private suspend fun runWhisperCpp(
        modelFilePath: String,
        waveFilePath: Path,
        documentDirectory: DocumentDirectory,
    ) {
        val outputLrcFilePath = documentDirectory.lrcFilePath().toString()
        val process = ProcessBuilder(
            "whisper-cpp",
            "--model",
            modelFilePath,
            "--output-lrc",
            "--output-file",
            outputLrcFilePath.replace(".lrc", ""),
            "--language",
            language,
            waveFilePath.toString(),
        ).start(captureStdout = false, captureStderr = false)
        val exitCode = process.waitUntil(kotlin.time.Duration.parse("60s"))
        if (exitCode != 0) {
            throw Exception("whisper-cpp failed with exit code $exitCode. Stderr: ${process.stderr?.slurpString()}")
        }

        if (fileExists(outputLrcFilePath)) {
            info("Transcribed to lrc file: file://$outputLrcFilePath")
        } else {
            throw Exception("Failed to transcribe to lrc file: file://$outputLrcFilePath")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun downloadModel(modelName: String): String {
        val baseUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"
        val modelUrl = "$baseUrl/ggml-$modelName.bin"

        val homedir = getHomeDirectory()
        val modelDirectory = "$homedir/Documents/models"
        mkdirP(modelDirectory)

        val modelPath = "$modelDirectory/ggml-$modelName.bin"

        if (fileExists(modelPath)) {
            return modelPath
        }

        info("Fetching model from $modelUrl to $modelPath")
        val response: HttpResponse = client.get(modelUrl)
        val responseBody = response.bodyAsChannel()

        withContext(Dispatchers.IO) {
            val file =
                fopen(modelPath, "wb") ?: throw RuntimeException("Cannot open file for writing: $modelPath")
            try {
                while (!responseBody.isClosedForRead) {
                    val buffer = ByteArray(4096)
                    val bytesRead = responseBody.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        fwrite(buffer.refTo(0), 1u, bytesRead.toULong(), file)
                    }
                }
            } finally {
                fclose(file)
            }
        }

        return modelPath
    }
}
