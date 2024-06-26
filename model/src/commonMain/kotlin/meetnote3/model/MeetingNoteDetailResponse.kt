package meetnote3.model

import kotlinx.serialization.Serializable

@Serializable
data class MeetingNoteDetailResponse(
    val summary: String?,
    val lrc: String?,
    val path: String,
)
