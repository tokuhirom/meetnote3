package meetnote3.service

import meetnote3.debug
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.model.DocumentStatus
import meetnote3.recorder.mix
import okio.FileSystem
import platform.posix.warn

import kotlinx.datetime.Clock

data class RecoveryLog(
    val documentDirectory: DocumentDirectory,
    var startAtSeconds: Long? = null,
    var endAtSeconds: Long? = null,
    var error: String? = null,
    var status: DocumentStatus = documentDirectory.status(),
) {
    fun updateStatus() {
        status = documentDirectory.status()
    }
}

class RecoveringService(
    private val transcriptService: WhisperTranscriptService,
    private val summarizeService: SummarizeService,
) {
    private val fs = FileSystem.SYSTEM
    private var currentTarget: RecoveryLog? = null
    private var recoveryLogs: List<RecoveryLog> = emptyList()

    fun recoveryLogs(): List<RecoveryLog> = recoveryLogs

    fun currentTarget(): RecoveryLog? = currentTarget

    suspend fun recover() {
        println("RecoveringService.recover")

        recoveryLogs = DocumentDirectory
            .listAll()
            .filter {
                !FileSystem.SYSTEM.exists(it.summaryFilePath())
            }.filter {
                !it.didRecoveryError()
            }.sortedBy {
                it.shortName()
            }.map {
                RecoveryLog(it)
            }.shuffled()

        info("Recovering: ${recoveryLogs.size} directories.")
        recoveryLogs.forEach { recoveryLog ->
            try {
                debug("Recovering: $recoveryLog")
                currentTarget = recoveryLog
                recoveryLog.startAtSeconds = Clock.System.now().epochSeconds
                recoverDocument(recoveryLog)
            } catch (e: Exception) {
                warn("Failed to recover: $recoveryLog $e")
                recoveryLog.documentDirectory.writeRecoveryError(e.toString() + "\n" + e.stackTraceToString())
                recoveryLog.error = e.toString()
            } finally {
                currentTarget = null
            }
        }
    }

    private suspend fun recoverDocument(recoveryLog: RecoveryLog) {
        val dd = recoveryLog.documentDirectory

        // create mixed file if not exists
        if (fs.exists(dd.micFilePath()) && fs.exists(dd.screenFilePath()) && !fs.exists(dd.mixedFilePath())) {
            info("Recovering mix: $dd")
            mix(
                listOf(
                    dd.micFilePath().toString(),
                    dd.screenFilePath().toString(),
                ),
                dd.mixedFilePath().toString(),
            )
            info("Mixed: $dd")
        }
        recoveryLog.updateStatus()

        if (fs.exists(dd.waveFilePath())) {
            info("Remove temporary file $dd")
            fs.delete(dd.waveFilePath())
        }
        recoveryLog.updateStatus()

        if (fs.exists(dd.mixedFilePath()) && !fs.exists(dd.lrcFilePath())) {
            transcriptService.transcribe(dd)
        }
        recoveryLog.updateStatus()

        if (fs.exists(dd.lrcFilePath()) && !fs.exists(dd.summaryFilePath())) {
            summarizeService.summarize(dd)
        }
        recoveryLog.updateStatus()
    }
}
