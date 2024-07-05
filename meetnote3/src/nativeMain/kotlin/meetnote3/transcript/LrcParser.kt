package meetnote3.transcript

import okio.FileSystem
import okio.Path

fun getLrcLastTimestamp(filePath: Path): String {
    // Sample input:
    //
    // [by:whisper.cpp]
    // [00:40.00]ご視聴ありがとうございました!
    FileSystem.SYSTEM.read(filePath) {
        val lines = readUtf8().split("\n")
        val lastLine = lines.dropLast(1).last()
        return lastLine.substring(1, 9)
    }
}

data class LrcLine(
    // Timestamp in the format of "mm:ss.SS"
    val timestamp: String,
    val content: String,
) {
    fun timestampToSeconds(): Double {
        val parts = timestamp.split(":", ".")
        val minutesToSeconds = parts[0].toDouble() * 60
        val seconds = parts[1].toDouble()
        val millisecondsToSeconds = parts[2].toDouble() / 100
        return minutesToSeconds + seconds + millisecondsToSeconds
    }
}

fun parseLrcContent(lrcContent: String): List<LrcLine> {
    val lines = lrcContent.lines()
    val lrcLines = mutableListOf<LrcLine>()
    var previousContent: String? = null

    for (line in lines) {
        if (line.startsWith("[") && line != "[by:whisper.cpp]") {
            val timestamp = line.substringAfter("[").substringBefore("]")
            val content = line.substringAfter("]").trim()

            if (content != previousContent) {
                lrcLines.add(LrcLine(timestamp, content))
                previousContent = content
            }
        }
    }

    return lrcLines
}
