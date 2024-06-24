package meetnote3.service

import meetnote3.model.DocumentDirectory
import meetnote3.python.SUMMARIZE_GPT2
import meetnote3.utils.ProcessBuilder
import okio.FileSystem
import okio.Path

import kotlin.time.Duration

class SummarizeService(
    private val summarizerCommandTimeout: Duration = Duration.parse("1h"),
) {
    private var installedLibraries = false

    suspend fun summarize(documentDirectory: DocumentDirectory) {
        println("SummarizeService.summarize")

        val pythonLibDir = installLibraries()

        val summaryFilePath = documentDirectory.summaryFilePath()

        // write python code to summarize the transcript
        val summarizerFilePath = documentDirectory.summarizerFilePath()
        FileSystem.SYSTEM.write(summarizerFilePath) {
            writeUtf8("import sys\n")
            writeUtf8("sys.path.insert(0, '$pythonLibDir')\n")
            writeUtf8(SUMMARIZE_GPT2)
        }

        try {
            val processBuilder =
                ProcessBuilder(
                    "python3",
                    summarizerFilePath.toString(),
                    documentDirectory.lrcFilePath().toString(),
                    summaryFilePath.toString(),
                )
            val process = processBuilder.start(captureStdout = false, captureStderr = false)
            process.waitUntil(summarizerCommandTimeout)
            println("Summary ready: file://$summaryFilePath")
        } finally {
            FileSystem.SYSTEM.delete(summarizerFilePath)
        }
    }

    private suspend fun installLibraries(): Path {
        val basedir = DocumentDirectory.baseDirectory()
        val pythonLibDir = basedir.resolve("pythonlib")

        if (installedLibraries) {
            return pythonLibDir
        }

        val processBuilder =
            ProcessBuilder(
                "pip3",
                "install",
                "--target=$pythonLibDir",
                "transformers",
                "torch",
                "sentencepiece",
                "protobuf",
            )
        val process = processBuilder.start(captureStdout = false, captureStderr = false)
        process.waitUntil(Duration.parse("1h"))
        installedLibraries = true
        return pythonLibDir
    }
}
