package meetnote3.server.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.model.MeetingLogEntity
import meetnote3.model.MeetingNoteDetailResponse
import meetnote3.service.SummarizeService
import meetnote3.transcript.getLrcLastTimestamp
import okio.FileSystem

fun Route.meetingLogRoutes(summarizeService: SummarizeService) {
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
        if (document == null) {
            call.respondText(ContentType.Text.Plain, HttpStatusCode.NotFound) {
                "Document not found."
            }
            return@get
        }

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
        call.respond(
            MeetingNoteDetailResponse(
                summary = summary,
                lrc = lrc,
                path = document.basedir.toString(),
                mixedAvailable = document.mixedFilePath().let(FileSystem.SYSTEM::exists),
                micAvailable = document.micFilePath().let(FileSystem.SYSTEM::exists),
                screenAvailable = document.screenFilePath().let(FileSystem.SYSTEM::exists),
                images = document.listImages().map {
                    it.name
                },
            ),
        )
    }

    post("/api/meeting-logs/{name}/summarize") {
        val meetingNote = call.parameters["name"]
        val document = DocumentDirectory.find(meetingNote!!)
        if (document == null) {
            call.respondText(ContentType.Text.Plain, HttpStatusCode.NotFound) {
                "Document not found."
            }
            return@post
        }

        summarizeService.summarize(document)
        call.respondText("OK", ContentType.Image.PNG)
    }

    get("/api/meeting-logs/{name}/images/{image}") {
        val meetingNote = call.parameters["name"]
        val imageFileName = call.parameters["image"]
        val document = DocumentDirectory.find(meetingNote!!)
        if (document == null) {
            call.respondText(ContentType.Text.Plain, HttpStatusCode.NotFound) {
                "Document not found."
            }
            return@get
        }
        val image = document.listImages().firstOrNull {
            it.name == imageFileName
        }
        if (image == null) {
            call.respondText(ContentType.Text.Plain, HttpStatusCode.NotFound) {
                "Image not found."
            }
            return@get
        }
        FileSystem.SYSTEM.read(image) {
            call.respondBytes(readByteArray(), ContentType.Image.PNG)
        }
    }

    listOf(
        "mixed" to { document: DocumentDirectory -> document.mixedFilePath() },
        "mic" to { document: DocumentDirectory -> document.micFilePath() },
        "screen" to { document: DocumentDirectory -> document.screenFilePath() },
    ).forEach { (fileType, pathSelector) ->
        get("/api/meeting-logs/{name}/$fileType") {
            val meetingNote = call.parameters["name"]
            val document = DocumentDirectory.find(meetingNote!!)
            if (document != null) {
                val path = pathSelector(document)
                if (FileSystem.SYSTEM.exists(path)) {
                    FileSystem.SYSTEM.read(path) {
                        call.respondBytes(readByteArray(), ContentType.Audio.MP4)
                    }
                } else {
                    call.respondText(ContentType.Text.Plain, HttpStatusCode.NotFound) {
                        "File not found."
                    }
                }
            } else {
                call.respondText(ContentType.Text.Plain, HttpStatusCode.NotFound) {
                    "Document not found."
                }
            }
        }
    }
}
