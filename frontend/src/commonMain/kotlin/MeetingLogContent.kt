import mui.material.Grid
import mui.system.responsive
import react.FC
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.p

val MeetingLogContent =
    FC {
        NavBar()
        Grid {
            this.container = true
            spacing = responsive(2)

            Grid {
                item = true
                xs = 4

                p {
                    +"Lorem ipsum dolor sit amet, consectetur adipiscing elit,"
                }
            }
            Grid {
                xs = 8

                h1 {
                    +"Meeting logs"
                }
            }
        }
    }
