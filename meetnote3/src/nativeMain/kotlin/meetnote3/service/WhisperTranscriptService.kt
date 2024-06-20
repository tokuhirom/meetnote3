package meetnote3.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.model.getHomeDirectory
import meetnote3.model.mkdirP
import meetnote3.utils.ProcessBuilder
import meetnote3.utils.fileExists
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.remove

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class WhisperTranscriptService(
    private val modelName: String = "small",
    private val language: String = "japanese",
) {
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
        } finally {
            // Clean up temporary wave file
            remove(waveFilePath)
        }
    }

    private suspend fun convertToWave(
        inputFilePath: String,
        outputFilePath: String,
    ) {
        val command = "ffmpeg -i $inputFilePath -ar 16000 -ac 1 -c:a pcm_s16le $outputFilePath"
        val process = ProcessBuilder(command).start(captureStdout = true, captureStderr = true)
        val exitCode = process.waitUntil(kotlin.time.Duration.parse("60s"))
        if (exitCode != 0) {
            throw Exception("ffmpeg failed with exit code $exitCode. Stderr: ${process.stderr?.slurpString()}")
        }
    }

    private suspend fun runWhisperCpp(
        modelFilePath: String,
        waveFilePath: String,
        documentDirectory: DocumentDirectory,
    ) {
        val outputLrcFilePath = documentDirectory.lrcFilePath()
        val command =
            "whisper-cpp --model $modelFilePath --output-lrc --output-file ${
                outputLrcFilePath.replace(
                    ".lrc",
                    "",
                )
            } --language $language $waveFilePath"
        val process = ProcessBuilder(command).start(captureStdout = false, captureStderr = false)
        val exitCode = process.waitUntil(kotlin.time.Duration.parse("60s"))
        if (exitCode != 0) {
            throw Exception("whisper-cpp failed with exit code $exitCode. Stderr: ${process.stderr?.slurpString()}")
        } else {
            info("Finished whisper-cpp successfully. VTT file: file://$outputLrcFilePath")
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
