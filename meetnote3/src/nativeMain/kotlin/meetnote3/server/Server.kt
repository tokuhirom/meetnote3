package meetnote3.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.Routing
import meetnote3.info
import meetnote3.server.routes.meetingLogRoutes
import meetnote3.server.routes.procsRoutes
import meetnote3.server.routes.staticContentRoutes
import meetnote3.server.routes.systemLogRoutes
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
    @OptIn(ExperimentalForeignApi::class)
    fun startServer(port: Int): Int {
        // listen on a random port.
        // host is 127.0.0.1 to avoid listening on all interfaces.
        // user can access the server by visiting http://localhost:<port>/
        val server = embeddedServer(CIO, port = port, host = "127.0.0.1", configure = {
            this.reuseAddress = true
        }) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                    },
                )
            }
            val cors = getenv("MEETNOTE3_CORS")?.toKString()
            if (cors != null) {
                install(CORS) {
                    allowHost(cors)
                }
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
