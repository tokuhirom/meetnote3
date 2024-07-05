package meetnote3.ui.meetinglog

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
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
    internal var currentTimeLabel: NSTextField? = null

    fun load(filePath: String) {
        val fileUrl = NSURL.fileURLWithPath(filePath)
        val playerItem = AVPlayerItem.playerItemWithURL(fileUrl)
        player = AVPlayer.playerWithPlayerItem(playerItem)
        startUpdatingCurrentTime()
    }

    fun play() {
        player?.play()
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
        this.play()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun startUpdatingCurrentTime() {
        stopUpdatingCurrentTime()

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
        val msg = secondsToString(seconds) + " / " + secondsToString(getDurationInSeconds())
        currentTimeLabel?.setStringValue(msg)
    }

    private fun secondsToString(seconds: Double): String {
        val minutes = seconds.toInt() / 60
        val remainingSeconds = seconds.toInt() % 60
        return "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
    }

    @OptIn(ExperimentalForeignApi::class)
    fun getDurationInSeconds(): Double {
        val duration = player?.currentItem?.duration ?: return 0.0
        return CMTimeGetSeconds(duration)
    }
}
