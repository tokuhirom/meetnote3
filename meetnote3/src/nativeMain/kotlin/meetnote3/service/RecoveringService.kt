package meetnote3.service

import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.recorder.mix
import okio.FileSystem

class RecoveringService {
    private val fs = FileSystem.SYSTEM
    private val whisperTranscriptService = WhisperTranscriptService()
    private val summarizeService = SummarizeService()

    suspend fun recover() {
        println("RecoveringService.recover")

        val dirs = DocumentDirectory.listAll()
        info("Recovering: ${dirs.size} directories.")
        dirs.forEach {
            try {
                info("Recovering: $it")
                recoverDocument(it)
            } catch (e: Exception) {
                info("Failed to recover: $it")
                e.printStackTrace()
            }
        }
    }

    private suspend fun recoverDocument(dd: DocumentDirectory) {
        // create mixed file if not exists
        if (fs.exists(dd.micFilePath()) && fs.exists(dd.screenFilePath()) && !fs.exists(dd.mixedFilePath())) {
            info("Recovering: $dd")
            mix(
                listOf(
                    dd.micFilePath().toString(),
                    dd.screenFilePath().toString(),
                ),
                dd.mixedFilePath().toString(),
            )
        }
        if (fs.exists(dd.waveFilePath())) {
            info("Remove temporary file $dd")
            fs.delete(dd.waveFilePath())
        }
        if (fs.exists(dd.mixedFilePath()) && !fs.exists(dd.lrcFilePath())) {
            whisperTranscriptService.transcribe(dd)
        }
        if (fs.exists(dd.lrcFilePath()) && !fs.exists(dd.summaryFilePath())) {
            summarizeService.summarize(dd)
        }
    }
}
