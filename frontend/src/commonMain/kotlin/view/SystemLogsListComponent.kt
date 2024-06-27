package view

import ApiClient
import mui.material.Grid
import mui.system.responsive
import react.FC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.pre
import react.router.dom.NavLink
import react.router.useParams
import react.use.useAsyncEffect
import react.useState
import xs

val SystemLogListSidebarComponent = FC {
    Grid {
        val (systemLogs, setSystemLogs) = useState<List<String>>(emptyList())
        useAsyncEffect {
            ApiClient().getSystemLogs().also {
                setSystemLogs(it)
            }
        }

        item = true
        xs = 3

        ReactHTML.ul {
            systemLogs.forEach {
                ReactHTML.li {
                    NavLink {
                        +"$it"
                        to = "/system-logs/$it"
                    }
                }
            }
        }
    }
}

val SystemLogsListComponent =
    FC {
        NavBar()

        Grid {
            this.container = true
            spacing = responsive(2)

            SystemLogListSidebarComponent()
            Grid {
                item = true
                xs = 9

                ReactHTML.h1 {
                    +"System logs"
                }
            }
        }
    }

val SystemLogsDetailComponent =
    FC {
        val params = useParams()
        val name = params["name"] ?: error("Missing name")

        NavBar()

        Grid {
            this.container = true
            spacing = responsive(2)

            SystemLogListSidebarComponent()
            Grid {
                item = true
                xs = 9

                val (systemLogDetail, setSystemLogDetail) = useState<String?>(
                    null,
                )
                useAsyncEffect(name) {
                    val meetingLogDetail = ApiClient().getSystemLogDetail(name)
                    setSystemLogDetail(meetingLogDetail)
                }

                ReactHTML.h1 {
                    +"System log: $name"
                }
                pre {
                    +systemLogDetail
                }
            }
        }
    }
