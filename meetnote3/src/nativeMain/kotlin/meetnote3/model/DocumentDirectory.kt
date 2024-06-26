package meetnote3.model

import meetnote3.info
import meetnote3.model.DocumentDirectory.Companion.dateTimeFormatter
import meetnote3.utils.getHomeDirectory
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

fun generateTimestamp(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.format(dateTimeFormatter)
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

    fun shortName(): String =
        dateTimeFormatter
            .parse(basedir.name)
            .format(shortNameFormatter)

    fun createImageFileName(): Path {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return basedir.resolve("images").resolve(now.format(dateTimeFormatter) + ".png")
    }

    fun listImages(): List<Path> =
        try {
            FileSystem.SYSTEM.list(basedir.resolve("images")).toList()
        } catch (e: FileNotFoundException) {
            info("Failed to create images directory: $e")
            emptyList()
        }

    companion object {
        fun create(): DocumentDirectory {
            val basedir = baseDirectory()
            val dateTimeString = generateTimestamp()
            val dir = basedir.resolve(dateTimeString)
            FileSystem.SYSTEM.createDirectories(dir)
            FileSystem.SYSTEM.createDirectories(dir.resolve("images"))
            return DocumentDirectory(dir)
        }

        fun baseDirectory(): Path {
            val home = getHomeDirectory()
            val directory = home.resolve("Documents/MeetNote3/")
            FileSystem.SYSTEM.createDirectories(directory)
            return directory
        }

        fun listAll(): List<DocumentDirectory> {
            val baseDirectory = baseDirectory()
            info("Listing all directories in $baseDirectory")
            return FileSystem.SYSTEM
                .list(baseDirectory)
                .filter {
                    FileSystem.SYSTEM.metadata(it).isDirectory
                }.filter {
                    it.name.startsWith("2")
                }.map {
                    DocumentDirectory(it)
                }.sortedBy {
                    it.basedir.toString()
                }
        }

        fun find(name: String): DocumentDirectory? = listAll().firstOrNull { it.basedir.name == name }

        val dateTimeFormatter = LocalDateTime.Format {
            date(
                LocalDate.Format {
                    year()
                    char('-')
                    monthNumber()
                    char('-')
                    dayOfMonth()
                },
            )
            char('T')
            time(
                LocalTime.Format {
                    hour()
                    char('-')
                    minute()
                    char('-')
                    second()
                },
            )
        }

        val shortNameFormatter =
            LocalDateTime.Format {
                date(
                    LocalDate.Format {
                        year()
                        char('-')
                        monthNumber()
                        char('-')
                        dayOfMonth()
                        char('(')
                        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                        char(')')
                    },
                )
                chars(" ")
                time(
                    LocalTime.Format {
                        hour()
                        char(':')
                        minute()
                    },
                )
            }
    }
}
