package meetnote3.server.routes

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.util.decodeBase64String
import meetnote3.static.FRONTEND_HTML
import meetnote3.static.FRONTEND_JS

fun Route.staticContentRoutes() {
    get("/") {
        call.respondText(FRONTEND_HTML, ContentType.Text.Html)
    }
    get("/frontend.js") {
        val p: String = FRONTEND_JS.decodeBase64String()
        println(p)
        call.respondText(p, ContentType.Text.JavaScript)
    }
}
