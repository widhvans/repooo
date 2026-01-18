package com.freetube.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a YouTube video with all its metadata
 */
data class VideoInfo(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelName: String,
    val channelAvatarUrl: String,
    val duration: Long, // in seconds
    val viewCount: Long,
    val likeCount: Long,
    val uploadDate: String,
    val streamUrl: String? = null,
    val isLive: Boolean = false,
    val isShort: Boolean = false,
    val category: String = "",
    val tags: List<String> = emptyList()
) {
    val formattedDuration: String
        get() {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
    
    val formattedViewCount: String
        get() = formatCount(viewCount)
    
    val formattedLikeCount: String
        get() = formatCount(likeCount)
    
    companion object {
        fun formatCount(count: Long): String {
            return when {
                count >= 1_000_000_000 -> String.format("%.1fB", count / 1_000_000_000.0)
                count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
                count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
                else -> count.toString()
            }
        }
    }
}

/**
 * Represents a video stream quality option
 */
data class VideoStream(
    val url: String,
    val format: String,
    val resolution: String,
    val quality: String,
    val bitrate: Int,
    val isVideoOnly: Boolean = false
)

/**
 * Represents an audio stream option
 */
data class AudioStream(
    val url: String,
    val format: String,
    val bitrate: Int,
    val quality: String
)

/**
 * Container for all stream data of a video
 */
data class StreamData(
    val videoStreams: List<VideoStream>,
    val audioStreams: List<AudioStream>,
    val videoOnlyStreams: List<VideoStream> = emptyList(),
    val dashMpdUrl: String? = null,
    val hlsUrl: String? = null
)

/**
 * Room entity for watch history
 */
@Entity(tableName = "watch_history")
data class WatchHistoryItem(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    val duration: Long,
    val watchedAt: Long = System.currentTimeMillis(),
    val watchPosition: Long = 0, // Resume position in ms
    val watchDuration: Long = 0 // Total watched time
)

/**
 * Room entity for watch later
 */
@Entity(tableName = "watch_later")
data class WatchLaterItem(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    val duration: Long,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for downloads
 */
@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    val duration: Long,
    val filePath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val quality: String,
    val status: DownloadStatus = DownloadStatus.PENDING
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}
