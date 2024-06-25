package meetnote3.server.routes

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import meetnote3.server.loadStaticResource
import meetnote3.static.FRONTEND_CSS
import meetnote3.static.FRONTEND_HTML
import meetnote3.static.FRONTEND_JS

fun Route.staticContentRoutes() {
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
}
