package meetnote3.server.response

import kotlinx.serialization.Serializable

@Serializable
data class MeetingNoteDetailResponse(
    val summary: String?,
    val lrc: String?,
)
