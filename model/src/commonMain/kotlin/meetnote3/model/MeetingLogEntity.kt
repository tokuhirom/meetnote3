package meetnote3.model

import kotlinx.serialization.Serializable

@Serializable
data class MeetingLogEntity(
    val name: String,
    val shortName: String,
    val duration: String?,
)
