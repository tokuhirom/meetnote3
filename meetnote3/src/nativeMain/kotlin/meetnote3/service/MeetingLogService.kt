package meetnote3.service

import meetnote3.info
import meetnote3.model.DocumentDirectory
import meetnote3.model.MeetingLogEntity
import meetnote3.transcript.getLrcLastTimestamp
import okio.FileSystem

fun listMeetingLogs(): List<MeetingLogEntity> =
    DocumentDirectory
        .listAll()
        .map { documentDirectory ->
            val duration = if (FileSystem.SYSTEM.exists(documentDirectory.lrcFilePath())) {
                try {
                    getLrcLastTimestamp(documentDirectory.lrcFilePath())
                } catch (e: Exception) {
                    info("Failed to get last timestamp: ${e.message}")
                    null
                }
            } else {
                null
            }

            MeetingLogEntity(
                name = documentDirectory.basedir.name,
                shortName = documentDirectory.shortName(),
                duration = duration?.substring(0, 5),
            )
        }.sortedByDescending {
            it.name
        }.toList()
