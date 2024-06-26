package meetnote3.utils

import meetnote3.model.generateTimestamp
import okio.FileSystem
import okio.Path

fun getSystemLogDirectory(): Path {
    val shareDir = XdgAppDirectories("meetnote3").getShareDir()
    return shareDir
}

fun createNewSystemLogPath(): Path {
    val logDirectory = getSystemLogDirectory()
    val logFile = logDirectory / "meetnote3-${generateTimestamp()}.log"
    FileSystem.Companion.SYSTEM.createDirectories(logFile.parent!!)
    return logFile
}

fun listSystemLogFiles(): List<Path> {
    val shareDir = XdgAppDirectories("meetnote3").getShareDir()
    val systemLogFiles = FileSystem.SYSTEM
        .list(shareDir)
        .filter {
            it.name.lowercase().endsWith(".log")
        }.filter {
            FileSystem.SYSTEM.metadata(it).isRegularFile
        }
    return systemLogFiles
}
