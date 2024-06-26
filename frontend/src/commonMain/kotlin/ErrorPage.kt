import emotion.react.css
import js.errors.JsError
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.router.useRouteError
import web.cssom.NamedColor

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
