package meetnote3

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

const val LOG_LEVEL_TRACE = 0
const val LOG_LEVEL_DEBUG = 1
const val LOG_LEVEL_INFO = 2
const val LOG_LEVEL_WARNING = 3
const val LOG_LEVEL_ERROR = 4

private var logLevel = LOG_LEVEL_INFO

@OptIn(ExperimentalForeignApi::class)
fun initLogger() {
    val logLevelStringOrNull = getenv("LOG_LEVEL")?.toKString()
    when (logLevelStringOrNull) {
        "TRACE" -> logLevel = LOG_LEVEL_TRACE
        "DEBUG" -> logLevel = LOG_LEVEL_DEBUG
        "INFO" -> logLevel = LOG_LEVEL_INFO
        "WARNING" -> logLevel = LOG_LEVEL_WARNING
        "ERROR" -> logLevel = LOG_LEVEL_ERROR
        else -> LOG_LEVEL_INFO
    }
}

fun setLogLevel(level: Int) {
    logLevel = level
}

fun trace(message: String) {
    if (logLevel <= LOG_LEVEL_TRACE) {
        println("[TRACE] $message")
    }
}

fun debug(message: String) {
    if (logLevel <= LOG_LEVEL_DEBUG) {
        println("[DEBUG] $message")
    }
}

fun info(message: String) {
    if (logLevel <= LOG_LEVEL_INFO) {
        println("[INFO] $message")
    }
}

fun warning(message: String) {
    if (logLevel <= LOG_LEVEL_WARNING) {
        eprintln("[WARNING] $message")
    }
}

fun error(message: String) {
    if (logLevel <= LOG_LEVEL_ERROR) {
        eprintln("[ERROR] $message")
    }
}
