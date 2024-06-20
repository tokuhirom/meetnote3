package meetnote3.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import meetnote3.model.DocumentDirectory
import meetnote3.model.getHomeDirectory
import meetnote3.model.mkdirP

class WhisperTranscriptService {
    val client = HttpClient(CIO) {
    }

    suspend fun transcribe(documentDirectory: DocumentDirectory) {
        val mixedFilePath = documentDirectory.mixedFilePath()

        // convert to wave file
        // ffmpeg -i input.mp3 -ar 16000 -ac 1 -c:a pcm_s16le output.wav
        // write to template file
        // TODO convert mixedFilePath to wave file by ffmpeg

        val modelFileName = downloadModel("base")

        // run whisper-cpp
        // --output-vtt
        // --output-file
        // --language japanese
        // TODO: implement here
    }

    private suspend fun downloadModel(modelName: String): String {
        val baseUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"
        val modelUrl = "$baseUrl/ggml-$modelName.bin"

        val homedir = getHomeDirectory()
        val modelDirectory = "$homedir/Documents/models"
        mkdirP(modelDirectory)

        val modelPath = "$modelDirectory/ggml-$modelName.bin"
        val resp = client.get(modelUrl)
        // TODO: save to the file.
        return modelPath
    }
}
