package com.freetube.app.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.extractor.ExtractorHelper
import com.freetube.app.data.extractor.YouTubeService
import com.freetube.app.data.local.WatchHistoryDao
import com.freetube.app.data.local.WatchLaterDao
import com.freetube.app.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val video: VideoInfo? = null,
    val streamData: StreamData? = null,
    val comments: List<CommentInfo> = emptyList(),
    val relatedVideos: List<VideoInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingComments: Boolean = false,
    val error: String? = null,
    val selectedQuality: String = "Auto",
    val isDescriptionExpanded: Boolean = false,
    val isInWatchLater: Boolean = false,
    val showQualityDialog: Boolean = false,
    val showSpeedDialog: Boolean = false,
    val playbackSpeed: Float = 1f
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val youtubeService: YouTubeService,
    private val watchHistoryDao: WatchHistoryDao,
    private val watchLaterDao: WatchLaterDao
) : ViewModel() {
    
    private val videoId: String = savedStateHandle.get<String>("videoId") ?: ""
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    val availableQualities = listOf(
        "Auto",
        "1080p",
        "720p",
        "480p",
        "360p",
        "240p",
        "144p"
    )
    
    val playbackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    
    init {
        loadVideo()
    }
    
    private fun loadVideo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val videoUrl = ExtractorHelper.buildVideoUrl(videoId)
            
            // Load video info
            youtubeService.getVideoInfo(videoUrl).fold(
                onSuccess = { video ->
                    _uiState.value = _uiState.value.copy(video = video)
                    
                    // Add to watch history
                    addToHistory(video)
                    
                    // Check if in watch later
                    checkWatchLater()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load video"
                    )
                    return@launch
                }
            )
            
            // Load stream data (the actual video URLs - ad-free!)
            youtubeService.getStreamData(videoUrl).fold(
                onSuccess = { streamData ->
                    _uiState.value = _uiState.value.copy(
                        streamData = streamData,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load streams"
                    )
                }
            )
            
            // Load related videos
            loadRelatedVideos(videoUrl)
            
            // Load comments
            loadComments(videoUrl)
        }
    }
    
    private suspend fun loadRelatedVideos(videoUrl: String) {
        youtubeService.getRelatedVideos(videoUrl).fold(
            onSuccess = { videos ->
                _uiState.value = _uiState.value.copy(relatedVideos = videos)
            },
            onFailure = { /* Ignore errors for related videos */ }
        )
    }
    
    private suspend fun loadComments(videoUrl: String) {
        _uiState.value = _uiState.value.copy(isLoadingComments = true)
        
        youtubeService.getComments(videoUrl).fold(
            onSuccess = { page ->
                _uiState.value = _uiState.value.copy(
                    comments = page.comments,
                    isLoadingComments = false
                )
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(isLoadingComments = false)
            }
        )
    }
    
    private suspend fun addToHistory(video: VideoInfo) {
        val historyItem = WatchHistoryItem(
            videoId = video.id,
            title = video.title,
            thumbnailUrl = video.thumbnailUrl,
            channelName = video.channelName,
            duration = video.duration
        )
        watchHistoryDao.insert(historyItem)
    }
    
    private suspend fun checkWatchLater() {
        val isInWatchLater = watchLaterDao.exists(videoId)
        _uiState.value = _uiState.value.copy(isInWatchLater = isInWatchLater)
    }
    
    fun toggleWatchLater() {
        viewModelScope.launch {
            val video = _uiState.value.video ?: return@launch
            
            if (_uiState.value.isInWatchLater) {
                watchLaterDao.deleteById(videoId)
                _uiState.value = _uiState.value.copy(isInWatchLater = false)
            } else {
                val item = WatchLaterItem(
                    videoId = video.id,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    channelName = video.channelName,
                    duration = video.duration
                )
                watchLaterDao.insert(item)
                _uiState.value = _uiState.value.copy(isInWatchLater = true)
            }
        }
    }
    
    fun toggleDescriptionExpanded() {
        _uiState.value = _uiState.value.copy(
            isDescriptionExpanded = !_uiState.value.isDescriptionExpanded
        )
    }
    
    fun setQuality(quality: String) {
        _uiState.value = _uiState.value.copy(
            selectedQuality = quality,
            showQualityDialog = false
        )
    }
    
    fun showQualityDialog() {
        _uiState.value = _uiState.value.copy(showQualityDialog = true)
    }
    
    fun hideQualityDialog() {
        _uiState.value = _uiState.value.copy(showQualityDialog = false)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(
            playbackSpeed = speed,
            showSpeedDialog = false
        )
    }
    
    fun showSpeedDialog() {
        _uiState.value = _uiState.value.copy(showSpeedDialog = true)
    }
    
    fun hideSpeedDialog() {
        _uiState.value = _uiState.value.copy(showSpeedDialog = false)
    }
    
    fun updateWatchPosition(positionMs: Long) {
        viewModelScope.launch {
            watchHistoryDao.updateWatchPosition(videoId, positionMs)
        }
    }
    
    /**
     * Get the best stream URL for playback
     */
    fun getBestStreamUrl(): String? {
        val streamData = _uiState.value.streamData ?: return null
        
        // Prefer HLS for adaptive streaming
        if (!streamData.hlsUrl.isNullOrEmpty()) {
            return streamData.hlsUrl
        }
        
        // Fall back to progressive video streams
        val selectedQuality = _uiState.value.selectedQuality
        
        // Try to match selected quality
        val matchingStream = streamData.videoStreams.find { 
            it.quality.contains(selectedQuality, ignoreCase = true) 
        }
        if (matchingStream != null) {
            return matchingStream.url
        }
        
        // Fall back to best available
        return streamData.videoStreams
            .sortedByDescending { it.bitrate }
            .firstOrNull()?.url
    }
    
    /**
     * Get audio stream for background playback
     */
    fun getBestAudioUrl(): String? {
        val streamData = _uiState.value.streamData ?: return null
        
        return streamData.audioStreams
            .sortedByDescending { it.bitrate }
            .firstOrNull()?.url
    }
}
