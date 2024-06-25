package meetnote3.server

import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import meetnote3.info
import meetnote3.server.routes.meetingLogRoutes
import meetnote3.server.routes.procsRoutes
import meetnote3.server.routes.staticContentRoutes
import meetnote3.server.routes.systemLogRoutes
import okio.FileSystem
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
                this.staticContentRoutes()
                this.meetingLogRoutes()
                this.systemLogRoutes()
                this.procsRoutes()
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
