package meetnote3.utils

import platform.posix.F_OK
import platform.posix.access

fun fileExists(fileName: String): Boolean = access(fileName, F_OK) == 0
