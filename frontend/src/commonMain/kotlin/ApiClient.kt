import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import meetnote3.model.MeetingLogEntity
import meetnote3.model.MeetingNoteDetailResponse
import meetnote3.model.ProcessInfo

class ApiClient(
    private val baseUrl: String = "http://localhost:9090",
) {
    private val client = HttpClient(Js) {
        install(ContentNegotiation) {
            json()
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    suspend fun getChildProcs(): List<ProcessInfo> {
        val response = client.get("$baseUrl/api/child-procs")
        return response.body<List<ProcessInfo>>()
    }

    suspend fun getMeetingLogs(): List<MeetingLogEntity> {
        val response = client.get("$baseUrl/api/meeting-logs")
        return response.body<List<MeetingLogEntity>>()
    }

    suspend fun getMeetingLogDetail(name: String): MeetingNoteDetailResponse {
        val response = client.get("$baseUrl/api/meeting-logs/$name")
        return response.body()
    }

    suspend fun getSystemLogs(): List<String> {
        val response = client.get("$baseUrl/api/system-logs")
        return response.body()
    }

    suspend fun getSystemLogDetail(name: String): String {
        val response = client.get("$baseUrl/api/system-logs/$name")
        return response.body()
    }
}
