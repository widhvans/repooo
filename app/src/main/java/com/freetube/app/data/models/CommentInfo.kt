package com.freetube.app.data.models

/**
 * Represents a YouTube comment
 */
data class CommentInfo(
    val id: String,
    val text: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val authorChannelId: String,
    val likeCount: Long,
    val publishedTime: String,
    val isHearted: Boolean = false,
    val isPinned: Boolean = false,
    val isAuthorChannelOwner: Boolean = false,
    val replyCount: Int = 0,
    val replies: List<CommentInfo> = emptyList()
) {
    val formattedLikeCount: String
        get() = VideoInfo.formatCount(likeCount)
}

/**
 * Container for comments with pagination
 */
data class CommentsPage(
    val comments: List<CommentInfo>,
    val nextPageToken: String?,
    val totalCommentCount: Long
)
