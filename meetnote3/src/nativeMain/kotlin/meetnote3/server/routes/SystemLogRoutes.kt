package meetnote3.server.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import meetnote3.utils.listSystemLogFiles
import okio.FileSystem
import okio.Path

fun Route.systemLogRoutes() {
    // system log list
    get("/api/system-logs") {
        val systemLogFiles = listSystemLogFiles()
            .sortedByDescending {
                it.name
            }.take(3)
            .map { it.name }
        call.respond(systemLogFiles)
    }
    // get system log detail
    get("/api/system-logs/{name}") {
        val systemLog = call.parameters["name"]
        val systemLogFile: Path? = listSystemLogFiles().find { it.name == systemLog }
        if (systemLogFile != null) {
            FileSystem.SYSTEM.read(systemLogFile) {
                call.respondText(readUtf8())
            }
        } else {
            call.respondText(ContentType.Text.Plain, HttpStatusCode.NotFound) {
                "System log file not found."
            }
        }
    }
}
