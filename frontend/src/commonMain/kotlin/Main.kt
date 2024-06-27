import mui.material.CssBaseline
import mui.material.GridProps
import react.FC
import react.Fragment
import react.create
import react.dom.client.createRoot
import react.router.Navigate
import react.router.RouteObject
import react.router.dom.RouterProvider
import react.router.dom.createHashRouter
import view.ErrorPage
import view.MeetingLogDetailComponent
import view.MeetingLogListComponent
import view.ProcsContent
import view.SystemLogsDetailComponent
import view.SystemLogsListComponent
import web.dom.document

// https://github.com/karakum-team/kotlin-mui-showcase/blob/5b7263a6a1379e40297f335f9e6be07e161dc9a7/src/jsMain/kotlin/team/karakum/MissedWrappers.kt#L8
inline var GridProps.xs: Int
    get() = TODO("Prop is write-only!")
    set(value) {
        asDynamic().xs = value
    }

fun main() {
    println("Hello, Kotlin/JS!")

    val container = document.getElementById("root")
        ?: error("Couldn't find root container!")
    createRoot(container).render(
        Fragment.create {
            CssBaseline()

            RouterProvider {
                router =
                    createHashRouter(
                        arrayOf(
                            RouteObject(
                                path = "/meeting-logs",
                                Component = MeetingLogListComponent,
                                ErrorBoundary = ErrorPage,
                            ),
                            RouteObject(
                                path = "/meeting-logs/:name",
                                Component = MeetingLogDetailComponent,
                                ErrorBoundary = ErrorPage,
                            ),
                            RouteObject(
                                path = "/system-logs",
                                Component = SystemLogsListComponent,
                                ErrorBoundary = ErrorPage,
                            ),
                            RouteObject(
                                path = "/system-logs/:name",
                                Component = SystemLogsDetailComponent,
                                ErrorBoundary = ErrorPage,
                            ),
                            RouteObject(
                                path = "/procs",
                                Component = ProcsContent,
                                ErrorBoundary = ErrorPage,
                            ),
                            RouteObject(
                                path = "*",
                                Component = FC {
                                    // Redirect to the root page if the path is not found
                                    Navigate {
                                        to = "/meeting-logs"
                                        replace = true
                                    }
                                },
                            ),
                        ),
                    )
            }
        },
    )
}
