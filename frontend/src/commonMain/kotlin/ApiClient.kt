import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
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
}
