package meetnote3.service

import meetnote3.debug
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.recorder.mix
import meetnote3.workers.SummarizingWorker
import meetnote3.workers.TranscriptWorker
import okio.FileSystem

class RecoveringService(
    private val transcriptWorker: TranscriptWorker,
    private val summarizingWorker: SummarizingWorker,
) {
    private val fs = FileSystem.SYSTEM

    suspend fun recover() {
        println("RecoveringService.recover")

        val dirs = DocumentDirectory.listAll()
        info("Recovering: ${dirs.size} directories.")
        dirs.forEach {
            try {
                debug("Recovering: $it")
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
            if ((FileSystem.SYSTEM.metadataOrNull(dd.screenFilePath())?.size ?: 0L) == 44L) {
                // A 44-byte file contains only the header and no data.
                // In this case, it is assumed that the file was not saved halfway, so recovery is skipped.
                debug("Incomplete file. Skip recovering: $dd")
            } else {
                mix(
                    listOf(
                        dd.micFilePath().toString(),
                        dd.screenFilePath().toString(),
                    ),
                    dd.mixedFilePath().toString(),
                )
            }
        }
        if (fs.exists(dd.waveFilePath())) {
            info("Remove temporary file $dd")
            fs.delete(dd.waveFilePath())
        }
        if (fs.exists(dd.mixedFilePath()) && !fs.exists(dd.lrcFilePath())) {
            transcriptWorker.emit(dd)
        }
        if (fs.exists(dd.lrcFilePath()) && !fs.exists(dd.summaryFilePath())) {
            summarizingWorker.emit(dd)
        }
    }
}
