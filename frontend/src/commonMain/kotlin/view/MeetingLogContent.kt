package view

import ApiClient
import meetnote3.model.MeetingLogEntity
import meetnote3.model.MeetingNoteDetailResponse
import mui.material.Grid
import mui.system.responsive
import react.FC
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.pre
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.router.dom.NavLink
import react.router.useParams
import react.use.useAsyncEffect
import react.useState
import xs

val MeetingLogListSidebarComponent = FC {
    Grid {
        val (meetingLogs, setMeetingLogs) = useState<List<MeetingLogEntity>>(emptyList())
        useAsyncEffect {
            ApiClient().getMeetingLogs().also {
                setMeetingLogs(it)
            }
        }

        item = true
        xs = 3

        table {
            tr {
                th {
                    +"Time"
                }
                th {
                    +"Duration"
                }
            }
        }
        meetingLogs.forEach {
            tr {
                td {
                    NavLink {
                        +"${it.shortName}"
                        to = "/meeting-logs/${it.name}"
                    }
                }
                th {
                    +"${it.duration}"
                }
            }
        }
    }
}

val MeetingLogListComponent =
    FC {
        NavBar()
        Grid {
            this.container = true
            spacing = responsive(2)

            MeetingLogListSidebarComponent()
            Grid {
                xs = 9

                h1 {
                    +"Meeting logs"
                }
            }
        }
    }

val MeetingLogDetailComponent =
    FC {
        NavBar()
        Grid {
            this.container = true
            spacing = responsive(2)

            val params = useParams()
            val name = params["name"] ?: error("Missing name")

            MeetingLogListSidebarComponent()
            Grid {
                xs = 9

                val (meetingLogDetail, setMeetingLogDetail) = useState<MeetingNoteDetailResponse?>(
                    null,
                )
                useAsyncEffect(name) {
                    val meetingLogDetail = ApiClient().getMeetingLogDetail(name)
                    setMeetingLogDetail(meetingLogDetail)
                }
                h1 {
                    +name
                }

                p {
                    +"Path: ${meetingLogDetail?.path}"
                }

                hr()
                h2 {
                    +"Summary"
                }
                pre {
                    +meetingLogDetail?.summary
                }

                hr()
                h2 {
                    +"Transcription"
                }
                pre {
                    +meetingLogDetail?.lrc
                }
            }
        }
    }
