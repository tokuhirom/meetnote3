package meetnote3.utils

import kotlinx.serialization.Serializable

@Serializable
data class ProcessInfo(
    val pid: Int,
    val ppid: Int,
    val name: String,
)
