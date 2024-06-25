package meetnote3.server.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import meetnote3.utils.getChildProcs

fun Route.procsRoutes() {
    get("/api/child-procs") {
        call.respond(getChildProcs())
    }
}
