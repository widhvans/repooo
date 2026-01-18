package com.freetube.app.data.models

/**
 * Represents a YouTube channel
 */
data class ChannelInfo(
    val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String,
    val bannerUrl: String,
    val subscriberCount: Long,
    val videoCount: Long,
    val isVerified: Boolean = false,
    val tabs: List<ChannelTab> = emptyList()
) {
    val formattedSubscriberCount: String
        get() = VideoInfo.formatCount(subscriberCount)
}

/**
 * Represents a tab on a channel page
 */
data class ChannelTab(
    val name: String,
    val type: ChannelTabType
)

enum class ChannelTabType {
    VIDEOS,
    SHORTS,
    LIVE,
    PLAYLISTS,
    COMMUNITY,
    CHANNELS,
    ABOUT
}

/**
 * Represents a subscription
 */
data class Subscription(
    val channelId: String,
    val channelName: String,
    val avatarUrl: String,
    val subscribedAt: Long = System.currentTimeMillis()
)
