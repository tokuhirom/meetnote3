import mui.material.AppBar
import mui.material.AppBarPosition
import mui.material.Button
import mui.material.ButtonColor
import mui.material.Toolbar
import react.FC
import web.location.location

val NavBar =
    FC {
        AppBar {
            position = AppBarPosition.static
            Toolbar {
                Button {
                    color = ButtonColor.inherit
                    +"Meeting Log"
                    onClick = {
                        location.href = "/#/meeting-logs"
                    }
                }
                Button {
                    color = ButtonColor.inherit
                    +"System Log"
                    onClick = {
                        location.href = "/#/system-logs"
                    }
                }
                Button {
                    color = ButtonColor.inherit
                    +"Procs"
                    onClick = {
                        location.href = "/#/procs"
                    }
                }
            }
        }
    }
