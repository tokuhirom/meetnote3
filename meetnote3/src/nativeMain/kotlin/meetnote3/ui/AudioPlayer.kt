package meetnote3.ui

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.seekToTime
import platform.AppKit.NSTextField
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_get_main_queue

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert

class AudioPlayer {
    private var player: AVPlayer? = null
    private var timeObserver: Any? = null

    fun play(filePath: String) {
        val fileUrl = NSURL.fileURLWithPath(filePath)
        val playerItem = AVPlayerItem.playerItemWithURL(fileUrl)
        player = AVPlayer.playerWithPlayerItem(playerItem)
        player?.play()

        // 再生時間の更新を開始
        startUpdatingCurrentTime()
    }

    fun pause() {
        player?.pause()
        stopUpdatingCurrentTime()
    }

    @OptIn(ExperimentalForeignApi::class)
    fun stop() {
        player?.pause()
        player?.seekToTime(CMTimeMake(value = 0, timescale = 1))
        stopUpdatingCurrentTime()
    }

    @OptIn(ExperimentalForeignApi::class)
    fun seekToTime(seconds: Double) {
        val time = CMTimeMakeWithSeconds(seconds, preferredTimescale = NSEC_PER_SEC.convert())
        player?.seekToTime(time)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun startUpdatingCurrentTime() {
        val interval = CMTimeMakeWithSeconds(1.0, preferredTimescale = NSEC_PER_SEC.convert())
        timeObserver = player?.addPeriodicTimeObserverForInterval(
            interval,
            queue = dispatch_get_main_queue(),
        ) { currentTime ->
            val seconds = CMTimeGetSeconds(currentTime)
            updateCurrentTimeDisplay(seconds)
        }
    }

    private fun stopUpdatingCurrentTime() {
        timeObserver?.let {
            player?.removeTimeObserver(it)
            timeObserver = null
        }
    }

    private fun updateCurrentTimeDisplay(seconds: Double) {
        val minutes = seconds.toInt() / 60
        val remainingSeconds = seconds.toInt() % 60
        val timeString =
            "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
        currentTimeLabel?.setStringValue(timeString)
    }

    companion object {
        var currentTimeLabel: NSTextField? = null
    }
}
