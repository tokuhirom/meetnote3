package meetnote3.workers

import meetnote3.model.DocumentDirectory
import meetnote3.service.WhisperTranscriptService
import platform.posix.warn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class TranscriptWorker(
    private val whisperTranscriptService: WhisperTranscriptService,
) {
    private var processingDocument: DocumentDirectory? = null
    private val sharedFlow = MutableSharedFlow<DocumentDirectory>()

    suspend fun emit(documentDirectory: DocumentDirectory) {
        sharedFlow.emit(documentDirectory)
    }

    fun start() {
        CoroutineScope(Dispatchers.Default).launch {
            sharedFlow.collect { documentDirectory ->
                println("Transcript ready: ${documentDirectory.lrcFilePath()}")
                try {
                    processingDocument = documentDirectory
                    whisperTranscriptService.transcribe(documentDirectory)
                } catch (e: Exception) {
                    warn("Cannot transcribe $documentDirectory: $e")
                } finally {
                    processingDocument = null
                }
            }
        }
    }
}
