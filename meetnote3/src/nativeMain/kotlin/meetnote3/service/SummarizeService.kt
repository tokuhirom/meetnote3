package meetnote3.service

import meetnote3.model.DocumentDirectory
import meetnote3.python.SUMMARIZE_GPT2
import meetnote3.utils.ProcessBuilder
import meetnote3.utils.spurt
import platform.posix.unlink

import kotlin.time.Duration

class SummarizeService {
    suspend fun summarize(documentDirectory: DocumentDirectory) {
        println("SummarizeService.summarize")

        val summaryFilePath = documentDirectory.summaryFilePath()

        // write python code to summarize the transcript

        val summarizerFilePath = documentDirectory.summarizerFilePath()
        spurt(summarizerFilePath, SUMMARIZE_GPT2)
        try {
            val processBuilder =
                ProcessBuilder("python3", summarizerFilePath, documentDirectory.lrcFilePath(), summaryFilePath)
            val process = processBuilder.start(captureStdout = false, captureStderr = false)
            process.waitUntil(Duration.parse("30s"))
            println("Summary ready: file://$summaryFilePath")
        } finally {
            unlink(summaryFilePath)
        }
    }
}
