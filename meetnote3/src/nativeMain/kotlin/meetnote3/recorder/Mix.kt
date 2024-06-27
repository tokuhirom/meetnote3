package meetnote3.recorder

import meetnote3.info
import platform.AVFoundation.AVAssetExportPresetAppleM4A
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatus
import platform.AVFoundation.AVAssetExportSessionStatusCancelled
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVAssetExportSessionStatusFailed
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVFileType
import platform.AVFoundation.AVFileTypeAppleM4A
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMutableComposition
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addMutableTrackWithMediaType
import platform.AVFoundation.tracksWithMediaType
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.kCMPersistentTrackID_Invalid
import platform.Foundation.NSURL
import platform.posix.warn

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
suspend fun mix(
    inputFileNames: List<String>,
    outputFileName: String,
    outputFileType: AVFileType = AVFileTypeAppleM4A,
    exportPresetName: String = AVAssetExportPresetAppleM4A,
) {
    val audioFiles = inputFileNames.map { NSURL.fileURLWithPath(it) }

    val composition = AVMutableComposition()

    audioFiles.forEachIndexed { _, fileURL ->
        val asset = AVURLAsset(fileURL, options = null)
        val assetTrack: AVAssetTrack? =
            asset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as AVAssetTrack?
        if (assetTrack != null) {
            val compositionTrack = composition.addMutableTrackWithMediaType(
                mediaType = AVMediaTypeAudio,
                preferredTrackID = kCMPersistentTrackID_Invalid,
            )
            val timeRange = CMTimeRangeMake(start = CMTimeMake(0, 1), duration = asset.duration)
            compositionTrack?.insertTimeRange(
                timeRange,
                ofTrack = assetTrack,
                atTime = CMTimeMake(0, 1),
                error = null,
            )
        }
    }

    val outputFileURL = NSURL.fileURLWithPath(outputFileName)
    val exporter = AVAssetExportSession(asset = composition, presetName = exportPresetName)
    exporter.outputURL = outputFileURL
    exporter.outputFileType = outputFileType

    when (val status = exporter.exportAsynchronously()) {
        AVAssetExportSessionStatusCompleted -> {
            info("Mixing completed successfully!")
        }

        AVAssetExportSessionStatusFailed, AVAssetExportSessionStatusCancelled -> {
            warn(
                "Failed to mix audio files: ${exporter.error?.localizedDescription}: $outputFileURL" +
                    " $inputFileNames",
            )
        }

        else -> {
            warn("Unknown export status: $status")
        }
    }
}

suspend fun AVAssetExportSession.exportAsynchronously(): AVAssetExportSessionStatus =
    suspendCoroutine { cont ->
        this.exportAsynchronouslyWithCompletionHandler {
            cont.resume(this.status)
        }
    }
