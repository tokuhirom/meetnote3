package meetnote3.workers

import meetnote3.model.DocumentDirectory
import meetnote3.service.WhisperTranscriptService
import platform.posix.warn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ProcessLog(
    val startAt: Long,
    var endAt: Long?,
    val documentDirectory: DocumentDirectory,
    var error: String? = null,
)

class TranscriptWorker(
    private val whisperTranscriptService: WhisperTranscriptService,
) {
    private val sharedFlow = MutableSharedFlow<DocumentDirectory>()
    private val processLogs = mutableListOf<ProcessLog>()
    private var waitingCount = 0

    fun processLogs(): List<ProcessLog> = processLogs

    fun waitingCount(): Int = waitingCount

    suspend fun emit(documentDirectory: DocumentDirectory) {
        waitingCount++
        sharedFlow.emit(documentDirectory)
    }

    fun start() {
        CoroutineScope(Dispatchers.Default).launch {
            sharedFlow.collect { documentDirectory ->
                println("Transcript ready: ${documentDirectory.lrcFilePath()}")
                val processLog = ProcessLog(
                    startAt = Clock.System.now().toEpochMilliseconds(),
                    endAt = null,
                    documentDirectory = documentDirectory,
                )
                processLogs.add(processLog)
                if (processLogs.size > 5) {
                    // Remove the oldest process log
                    processLogs.removeAt(0)
                }

                try {
                    whisperTranscriptService.transcribe(documentDirectory)
                    processLog.endAt = Clock.System.now().toEpochMilliseconds()
                } catch (e: Exception) {
                    warn("Cannot transcribe $documentDirectory: $e")
                    processLog.error = e.toString()
                } finally {
                    waitingCount--
                }
            }
        }
    }
}
