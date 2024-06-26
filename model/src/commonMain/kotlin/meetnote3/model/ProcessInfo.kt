package meetnote3.model

import kotlinx.serialization.Serializable

@Serializable
data class ProcessInfo(
    val pid: Int,
    val ppid: Int,
    val name: String,
)
