package meetnote3.server

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import meetnote3.info

import kotlinx.coroutines.runBlocking

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
            // ktor-server-call-logging is not supported on kotlin native@2.3.12.
            install(RequestLoggingPlugin)
            install(Routing) {
                get("/") {
                    call.respondText("Hello, Meetnote3!", ContentType.Text.Html)
                }
            }
        }
        server.start(wait = false)
        return runBlocking { server.resolvedConnectors().first().port }
    }
}
