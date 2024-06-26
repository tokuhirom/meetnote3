import emotion.react.css
import js.errors.JsError
import mui.material.AppBar
import mui.material.AppBarPosition
import mui.material.Button
import mui.material.ButtonColor
import mui.material.Toolbar
import react.FC
import react.Fragment
import react.Props
import react.create
import react.dom.client.createRoot
import react.dom.html.ReactHTML.div
import react.router.RouteObject
import react.router.dom.RouterProvider
import react.router.dom.createHashRouter
import react.router.useRouteError
import web.cssom.NamedColor
import web.dom.document

val NavBar =
    FC {
        AppBar {
            position = AppBarPosition.static
            Toolbar {
                listOf("top", "about", "contact").forEach { name ->
                    Button {
                        color = ButtonColor.inherit
                        +name.replaceFirstChar { it.titlecase() }
                    }
                }
            }
        }
    }

val RootContent =
    FC {
        NavBar()
        div {
            +"Root"
        }
    }
val AboutContent =
    FC {
        NavBar()
        div {
            +"About page"
        }
    }
val ErrorPage =
    FC<Props> {
        val error = useRouteError().unsafeCast<JsError>()

        div {
            css {
                color = NamedColor.red
            }
            +error.message
        }
    }

fun main() {
    println("Hello, Kotlin/JS!")

    val container = document.getElementById("root")
        ?: error("Couldn't find root container!")
    createRoot(container).render(
        Fragment.create {
            RouterProvider {
                router =
                    createHashRouter(
                        arrayOf(
                            RouteObject(
                                path = "/",
                                Component = RootContent,
                                ErrorBoundary = ErrorPage,
                            ),
                            RouteObject(
                                path = "/about",
                                Component = AboutContent,
                                ErrorBoundary = ErrorPage,
                            ),
                        ),
                    )
            }
        },
    )
}
