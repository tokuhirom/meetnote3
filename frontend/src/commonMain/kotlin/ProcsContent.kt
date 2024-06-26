import meetnote3.model.ProcessInfo
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.p
import react.use.useAsyncEffect
import react.useState

val ProcsContent =
    FC {
        NavBar()
        div {
            val (procs, setProcs) = useState<List<ProcessInfo>>(emptyList())
            useAsyncEffect {
                ApiClient().getChildProcs().also {
                    setProcs(it)
                }
            }

            h1 {
                +"Child process list"
            }

            p {
                +"Current child process count: ${procs.size}"
            }

            procs.forEach {
                div {
                    +"${it.pid} ${it.name}"
                }
            }
        }
    }
