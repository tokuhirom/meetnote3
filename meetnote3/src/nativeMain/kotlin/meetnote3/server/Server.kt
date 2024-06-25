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
import meetnote3.model.DocumentDirectory
import meetnote3.utils.getChildProcs
import meetnote3.utils.listSystemLogFiles

import kotlinx.coroutines.runBlocking
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.li
import kotlinx.html.stream.createHTML
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.ul

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
                    val src = createHTML().html {
                        head {
                            // load bootstrap css from CDN.
                            title { +"Meetnote3" }
                            style {
                                +"""
        .row {
            display: grid;
            grid-template-columns: 1fr 1fr 1fr;
            gap: 10px; /* カラム間の隙間 */
            padding: 5px;
        }
        @media (max-width: 800px) {
            .row {
                grid-template-columns: 1fr;
            }
        }
                                """.trimIndent()
                            }
                        }
                        body {
                            h1 { +"Hello, Meetnote3!" }
                            div("row") {
                                div("column") {
                                    h2 {
                                        +"Current Child Procs"
                                    }
                                    ul {
                                        getChildProcs().forEach {
                                            li { +(it.pid.toString() + " " + it.name) }
                                        }
                                    }
                                }
                                div("column") {
                                    h2 {
                                        +"System Logs"
                                    }
                                    ul {
                                        listSystemLogFiles().forEach {
                                            li { +it.name }
                                        }
                                    }
                                }
                                div("column") {
                                    h2 {
                                        +"Meeting Logs"
                                    }
                                    ul {
                                        DocumentDirectory.listAll().forEach {
                                            li { +it.basedir.name }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    call.respondText(src, ContentType.Text.Html)
                }
            }
        }
        server.start(wait = false)
        return runBlocking { server.resolvedConnectors().first().port }
    }
}
