package meetnote3.workers

import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.service.SummarizeService
import platform.posix.warn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class SummarizingWorker(
    private val summarizeService: SummarizeService,
) {
    private val sharedFlow = MutableSharedFlow<DocumentDirectory>()
    private var processingDocument: DocumentDirectory? = null

    suspend fun emit(documentDirectory: DocumentDirectory) {
        sharedFlow.emit(documentDirectory)
    }

    fun start() {
        info("SummarizingWorker started.")

        CoroutineScope(Dispatchers.Default).launch {
            sharedFlow.collect { documentDirectory ->
                println("Summarize ready: ${documentDirectory.lrcFilePath()}")
                try {
                    processingDocument = documentDirectory
                    summarizeService.summarize(documentDirectory)
                } catch (e: Exception) {
                    warn("Cannot summarize $documentDirectory: $e")
                } finally {
                    processingDocument = null
                }
            }
        }
    }
}
