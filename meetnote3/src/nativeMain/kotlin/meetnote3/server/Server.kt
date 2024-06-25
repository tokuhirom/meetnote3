package meetnote3.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.server.response.MeetingLogEntity
import meetnote3.server.response.MeetingNoteDetailResponse
import meetnote3.static.FRONTEND_CSS
import meetnote3.static.FRONTEND_HTML
import meetnote3.static.FRONTEND_JS
import meetnote3.utils.getChildProcs
import meetnote3.utils.listSystemLogFiles
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

// https://ktor.io/docs/server-custom-plugins.html#on-call
val RequestLoggingPlugin = createApplicationPlugin(name = "RequestLoggingPlugin") {
    onCall { call ->
        call.request.apply {
            info("[ktor-server] Request URL: ${local.method.value} ${local.uri}")
        }
    }
}

class Server {
    // return the port number.
    fun startServer(): Int {
        // listen on a random port.
        // host is 127.0.0.1 to avoid listening on all interfaces.
        // user can access the server by visiting http://localhost:<port>/
        val server = embeddedServer(CIO, port = 0, host = "127.0.0.1") {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                    },
                )
            }

            // ktor-server-call-logging is not supported on kotlin native@2.3.12.
            install(RequestLoggingPlugin)
            install(Routing) {
                get("/") {
                    loadStaticResource(
                        call,
                        ContentType.Text.Html,
                        "MEETNOTE3_HTML_DEBUG",
                        FRONTEND_HTML,
                    )
                }
                get("/frontend.js") {
                    loadStaticResource(
                        call,
                        ContentType.Text.JavaScript,
                        "MEETNOTE3_JS_DEBUG",
                        FRONTEND_JS,
                    )
                }
                get("/frontend.css") {
                    loadStaticResource(
                        call,
                        ContentType.Text.CSS,
                        "MEETNOTE3_CSS_DEBUG",
                        FRONTEND_CSS,
                    )
                }

                // API endpoints.
                get("/api/meeting-logs") {
                    val documents = DocumentDirectory
                        .listAll()
                        .map {
                            MeetingLogEntity(it.basedir.name)
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
                // meeting-note detail.
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
                get("/api/child-procs") {
                    call.respond(getChildProcs())
                }
            }
        }
        server.start(wait = false)
        return runBlocking { server.resolvedConnectors().first().port }
    }
}

@OptIn(ExperimentalForeignApi::class)
suspend fun loadStaticResource(
    call: ApplicationCall,
    contentType: ContentType,
    debugEnvironmentVariable: String,
    javascriptSource: String,
) {
    val debugFile = getenv(debugEnvironmentVariable)?.toKString()
    if (debugFile != null) {
        info("Loading frontend.js file from '$debugFile'")
        FileSystem.SYSTEM.read(debugFile.toPath()) {
            call.respondText(readUtf8(), contentType)
        }
    } else {
        call.respondText(javascriptSource, contentType)
    }
}
