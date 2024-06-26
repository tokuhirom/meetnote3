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
        println("XXXXXXXX $filePath $lastLine")
        return lastLine.substring(1, 9)
    }
}
