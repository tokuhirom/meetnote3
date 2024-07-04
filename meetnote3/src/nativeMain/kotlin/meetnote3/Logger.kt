package meetnote3

import platform.posix.getenv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
        println("${timestamp()} [TRACE] $message")
    }
}

fun debug(message: String) {
    if (logLevel <= LOG_LEVEL_DEBUG) {
        println("${timestamp()} [DEBUG] $message")
    }
}

fun info(message: String) {
    if (logLevel <= LOG_LEVEL_INFO) {
        println("${timestamp()} [INFO] $message")
    }
}

fun warn(message: String) {
    if (logLevel <= LOG_LEVEL_WARNING) {
        eprintln("${timestamp()} [WARNING] $message")
    }
}

fun error(message: String) {
    if (logLevel <= LOG_LEVEL_ERROR) {
        eprintln("${timestamp()} [ERROR] $message")
    }
}

private fun timestamp(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.toString()
}
