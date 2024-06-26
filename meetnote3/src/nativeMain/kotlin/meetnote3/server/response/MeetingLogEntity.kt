package meetnote3.server.response

import kotlinx.serialization.Serializable

@Serializable
data class MeetingLogEntity(
    val name: String,
    val shortName: String,
    val duration: String?,
)
