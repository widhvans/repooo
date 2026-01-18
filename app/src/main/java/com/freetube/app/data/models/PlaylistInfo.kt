package com.freetube.app.data.models

/**
 * Represents a YouTube playlist
 */
data class PlaylistInfo(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelName: String,
    val videoCount: Int,
    val videos: List<VideoInfo> = emptyList()
)

/**
 * Represents a user-created local playlist
 */
data class LocalPlaylist(
    val id: Long,
    val name: String,
    val thumbnailUrl: String?,
    val videoIds: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val videoCount: Int get() = videoIds.size
}
