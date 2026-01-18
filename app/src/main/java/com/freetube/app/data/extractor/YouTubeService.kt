package com.freetube.app.data.extractor

import com.freetube.app.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.channel.ChannelInfo as ExtractorChannelInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo as ExtractorPlaylistInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service class for fetching YouTube data using NewPipe Extractor
 * This is the core of ad-free video extraction
 */
@Singleton
class YouTubeService @Inject constructor() {
    
    private val service = ServiceList.YouTube
    
    /**
     * Get trending/popular videos
     */
    suspend fun getTrendingVideos(): Result<List<VideoInfo>> = withContext(Dispatchers.IO) {
        try {
            val kiosk = service.kioskList.getDefaultKioskExtractor(null)
            kiosk.fetchPage()
            
            val videos = kiosk.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoInfo() }
            
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get more trending videos with pagination
     */
    suspend fun getMoreTrendingVideos(nextPage: org.schabi.newpipe.extractor.Page?): Result<Pair<List<VideoInfo>, org.schabi.newpipe.extractor.Page?>> = withContext(Dispatchers.IO) {
        try {
            if (nextPage == null) return@withContext Result.failure(Exception("No more pages"))
            
            val kiosk = service.kioskList.getDefaultKioskExtractor(null)
            val page = kiosk.getPage(nextPage)
            
            val videos = page.items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoInfo() }
            
            Result.success(Pair(videos, page.nextPage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get video details by URL
     */
    suspend fun getVideoInfo(videoUrl: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(service, videoUrl)
            
            val video = VideoInfo(
                id = streamInfo.id,
                title = streamInfo.name,
                description = streamInfo.description?.content ?: "",
                thumbnailUrl = streamInfo.thumbnails.lastOrNull()?.url ?: "",
                channelId = streamInfo.uploaderUrl?.substringAfterLast("/") ?: "",
                channelName = streamInfo.uploaderName ?: "",
                channelAvatarUrl = streamInfo.uploaderAvatars.lastOrNull()?.url ?: "",
                duration = streamInfo.duration,
                viewCount = streamInfo.viewCount,
                likeCount = streamInfo.likeCount,
                uploadDate = streamInfo.uploadDate?.offsetDateTime()?.toString() ?: streamInfo.textualUploadDate ?: "",
                isLive = streamInfo.streamType == org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM,
                isShort = streamInfo.duration < 60,
                category = streamInfo.category ?: "",
                tags = streamInfo.tags ?: emptyList()
            )
            
            Result.success(video)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get video streams (ad-free!)
     */
    suspend fun getStreamData(videoUrl: String): Result<StreamData> = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(service, videoUrl)
            
            val videoStreams = streamInfo.videoStreams.map { stream ->
                VideoStream(
                    url = stream.content,
                    format = stream.format?.name ?: "Unknown",
                    resolution = stream.resolution ?: "Unknown",
                    quality = stream.resolution ?: "Unknown",
                    bitrate = stream.bitrate,
                    isVideoOnly = false
                )
            }
            
            val videoOnlyStreams = streamInfo.videoOnlyStreams.map { stream ->
                VideoStream(
                    url = stream.content,
                    format = stream.format?.name ?: "Unknown",
                    resolution = stream.resolution ?: "Unknown",
                    quality = stream.resolution ?: "Unknown",
                    bitrate = stream.bitrate,
                    isVideoOnly = true
                )
            }
            
            val audioStreams = streamInfo.audioStreams.map { stream ->
                AudioStream(
                    url = stream.content,
                    format = stream.format?.name ?: "Unknown",
                    bitrate = stream.bitrate,
                    quality = "${stream.bitrate / 1000}kbps"
                )
            }
            
            val streamData = StreamData(
                videoStreams = videoStreams,
                audioStreams = audioStreams,
                videoOnlyStreams = videoOnlyStreams,
                dashMpdUrl = streamInfo.dashMpdUrl,
                hlsUrl = streamInfo.hlsUrl
            )
            
            Result.success(streamData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search YouTube
     */
    suspend fun search(query: String, filters: SearchFilters = SearchFilters()): Result<SearchResultPage> = withContext(Dispatchers.IO) {
        try {
            val searchInfo = SearchInfo.getInfo(service, service.searchQHFactory.fromQuery(query))
            
            val results = searchInfo.getRelatedItems().mapNotNull { item ->
                when (item) {
                    is StreamInfoItem -> SearchResult.Video(item.toVideoInfo())
                    is ChannelInfoItem -> SearchResult.Channel(item.toChannelInfo())
                    is PlaylistInfoItem -> SearchResult.Playlist(item.toPlaylistInfo())
                    else -> null
                }
            }
            
            val page = SearchResultPage(
                results = results,
                nextPageToken = searchInfo.nextPage?.url,
                estimatedResultCount = results.size.toLong()
            )
            
            Result.success(page)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get search suggestions
     */
    suspend fun getSearchSuggestions(query: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val extractor = service.suggestionExtractor
            val suggestions = extractor.suggestionList(query)
            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get channel information
     */
    suspend fun getChannelInfo(channelUrl: String): Result<ChannelInfo> = withContext(Dispatchers.IO) {
        try {
            val channelInfo = ExtractorChannelInfo.getInfo(service, channelUrl)
            
            val channel = ChannelInfo(
                id = channelInfo.id,
                name = channelInfo.name,
                description = channelInfo.description ?: "",
                avatarUrl = channelInfo.avatars.lastOrNull()?.url ?: "",
                bannerUrl = channelInfo.banners.lastOrNull()?.url ?: "",
                subscriberCount = channelInfo.subscriberCount,
                videoCount = -1, // Not always available
                isVerified = channelInfo.isVerified
            )
            
            Result.success(channel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get channel videos
     */
    suspend fun getChannelVideos(channelUrl: String): Result<List<VideoInfo>> = withContext(Dispatchers.IO) {
        try {
            val channelInfo = ExtractorChannelInfo.getInfo(service, channelUrl)
            
            val videos = channelInfo.getRelatedItems()
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoInfo() }
            
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get video comments
     */
    suspend fun getComments(videoUrl: String): Result<CommentsPage> = withContext(Dispatchers.IO) {
        try {
            val commentsInfo = CommentsInfo.getInfo(service, videoUrl)
            
            val comments = commentsInfo.getRelatedItems().map { comment ->
                CommentInfo(
                    id = comment.commentId ?: "",
                    text = comment.commentText?.content ?: "",
                    authorName = comment.uploaderName ?: "",
                    authorAvatarUrl = comment.uploaderAvatars.lastOrNull()?.url ?: "",
                    authorChannelId = comment.uploaderUrl?.substringAfterLast("/") ?: "",
                    likeCount = comment.likeCount.toLong(),
                    publishedTime = comment.textualUploadDate ?: "",
                    isHearted = comment.isHeartedByUploader,
                    isPinned = comment.isPinned,
                    replyCount = comment.replyCount
                )
            }
            
            val page = CommentsPage(
                comments = comments,
                nextPageToken = commentsInfo.nextPage?.url,
                totalCommentCount = commentsInfo.commentsCount.toLong()
            )
            
            Result.success(page)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get playlist info
     */
    suspend fun getPlaylistInfo(playlistUrl: String): Result<PlaylistInfo> = withContext(Dispatchers.IO) {
        try {
            val playlistInfo = ExtractorPlaylistInfo.getInfo(service, playlistUrl)
            
            val videos = playlistInfo.getRelatedItems()
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoInfo() }
            
            val playlist = PlaylistInfo(
                id = playlistInfo.id,
                title = playlistInfo.name,
                description = playlistInfo.description?.content ?: "",
                thumbnailUrl = playlistInfo.thumbnails.lastOrNull()?.url ?: "",
                channelId = playlistInfo.uploaderUrl?.substringAfterLast("/") ?: "",
                channelName = playlistInfo.uploaderName ?: "",
                videoCount = playlistInfo.streamCount.toInt(),
                videos = videos
            )
            
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get related videos for a video
     */
    suspend fun getRelatedVideos(videoUrl: String): Result<List<VideoInfo>> = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(service, videoUrl)
            
            val videos = streamInfo.getRelatedItems()
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoInfo() }
            
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Extension functions to convert NewPipe models to our models
    
    private fun StreamInfoItem.toVideoInfo(): VideoInfo {
        return VideoInfo(
            id = url.substringAfterLast("=").substringAfterLast("/"),
            title = name,
            description = "",
            thumbnailUrl = thumbnails.lastOrNull()?.url ?: "",
            channelId = uploaderUrl?.substringAfterLast("/") ?: "",
            channelName = uploaderName ?: "",
            channelAvatarUrl = uploaderAvatars.lastOrNull()?.url ?: "",
            duration = duration,
            viewCount = viewCount,
            likeCount = 0,
            uploadDate = textualUploadDate ?: "",
            isLive = streamType == org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM,
            isShort = isShortFormContent
        )
    }
    
    private fun ChannelInfoItem.toChannelInfo(): ChannelInfo {
        return ChannelInfo(
            id = url.substringAfterLast("/"),
            name = name,
            description = description ?: "",
            avatarUrl = thumbnails.lastOrNull()?.url ?: "",
            bannerUrl = "",
            subscriberCount = subscriberCount,
            videoCount = streamCount,
            isVerified = isVerified
        )
    }
    
    private fun PlaylistInfoItem.toPlaylistInfo(): PlaylistInfo {
        return PlaylistInfo(
            id = url.substringAfterLast("list=").substringBefore("&"),
            title = name,
            description = description?.toString() ?: "",
            thumbnailUrl = thumbnails.lastOrNull()?.url ?: "",
            channelId = uploaderUrl?.substringAfterLast("/") ?: "",
            channelName = uploaderName ?: "",
            videoCount = streamCount.toInt()
        )
    }
}
