package meetnote3.service

import meetnote3.model.DocumentDirectory
import meetnote3.python.SUMMARIZE_GPT2
import meetnote3.utils.ProcessBuilder
import meetnote3.utils.spurt
import okio.FileSystem

import kotlin.time.Duration

class SummarizeService {
    suspend fun summarize(documentDirectory: DocumentDirectory) {
        println("SummarizeService.summarize")

        val summaryFilePath = documentDirectory.summaryFilePath()

        // write python code to summarize the transcript

        val summarizerFilePath = documentDirectory.summarizerFilePath()
        spurt(summarizerFilePath.toString(), SUMMARIZE_GPT2)
        try {
            val processBuilder =
                ProcessBuilder(
                    "python3",
                    summarizerFilePath.toString(),
                    documentDirectory.lrcFilePath().toString(),
                    summaryFilePath.toString(),
                )
            val process = processBuilder.start(captureStdout = false, captureStderr = false)
            process.waitUntil(Duration.parse("30s"))
            println("Summary ready: file://$summaryFilePath")
        } finally {
            FileSystem.SYSTEM.delete(summarizerFilePath)
        }
    }
}
