package meetnote3.model

import meetnote3.utils.getHomeDirectory
import meetnote3.utils.mkdirP
import okio.Path
import okio.Path.Companion.toPath

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun generateTimestamp(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val dateTimeString = now.toString().replace(":", "-")
    return dateTimeString
}

data class DocumentDirectory(
    val basedir: Path,
) {
    // Temporary audio file. Recorded voice from the microphone.
    // This file is removed after the mixing process.
    // I mean, mixedFilePath is created by mixing micFilePath and screenFilePath.
    fun micFilePath() = basedir.resolve("mic.m4a")

    // ditto.
    fun screenFilePath() = basedir.resolve("screen.m4a")

    // This file never remove automatically.
    fun mixedFilePath() = basedir.resolve("mixed.m4a")

    // This file is generated by the whisper-cpp.
    // And it's generated from the mixedFilePath.
    fun lrcFilePath() = basedir.resolve("transcribed.lrc")

    // This file is converted from the mixedFilePath.
    // Since the whisper-cpp requires the wave file.
    // We need to remove this after the transcribing process.
    fun waveFilePath() = basedir.resolve("mixed.wav")

    fun summaryFilePath() = basedir.resolve("summary.txt")

    fun summarizerFilePath() = basedir.resolve("summarizer.py")

    companion object {
        fun create(): DocumentDirectory = DocumentDirectory(baseDirectory())

        private fun baseDirectory(): Path {
            val home = getHomeDirectory()
            val dateTimeString = generateTimestamp()
            val directory = "$home/Documents/MeetNote3/$dateTimeString"
            mkdirP(directory)
            return directory.toPath()
        }
    }
}
