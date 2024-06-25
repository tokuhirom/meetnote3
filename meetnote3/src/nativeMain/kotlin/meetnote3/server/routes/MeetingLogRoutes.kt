package meetnote3.server.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.server.response.MeetingLogEntity
import meetnote3.server.response.MeetingNoteDetailResponse
import meetnote3.transcript.getLrcLastTimestamp
import okio.FileSystem

fun Route.meetingLogRoutes() {
    // meeting log list.
    get("/api/meeting-logs") {
        val documents = DocumentDirectory
            .listAll()
            .map { documentDirectory ->
                val duration = if (FileSystem.SYSTEM.exists(documentDirectory.lrcFilePath())) {
                    try {
                        getLrcLastTimestamp(documentDirectory.lrcFilePath())
                    } catch (e: Exception) {
                        info("Failed to get last timestamp: ${e.message}")
                        null
                    }
                } else {
                    null
                }

                MeetingLogEntity(
                    name = documentDirectory.basedir.name,
                    shortName = documentDirectory.shortName(),
                    duration = duration?.substring(0, 5),
                )
            }.sortedByDescending {
                it.name
            }.toList()
        call.respond(documents)
    }
    // get meeting note detail
    get("/api/meeting-logs/{name}") {
        val meetingNote = call.parameters["name"]
        val document = DocumentDirectory.find(meetingNote!!)
        if (document != null) {
            val summary = try {
                FileSystem.SYSTEM.read(document.summaryFilePath()) {
                    readUtf8()
                }
            } catch (e: Exception) {
                info("Summary file not found: ${e.message}")
                null
            }
            val lrc = try {
                FileSystem.SYSTEM.read(document.lrcFilePath()) {
                    readUtf8()
                }
            } catch (e: Exception) {
                info("Summary file not found: ${e.message}")
                null
            }
            call.respond(MeetingNoteDetailResponse(summary = summary, lrc = lrc))
        } else {
            call.respondText(ContentType.Text.Plain, HttpStatusCode.NotFound) {
                "Document not found."
            }
        }
    }
}
