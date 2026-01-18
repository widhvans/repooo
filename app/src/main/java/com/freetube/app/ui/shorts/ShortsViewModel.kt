package com.freetube.app.ui.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.extractor.ExtractorHelper
import com.freetube.app.data.extractor.YouTubeService
import com.freetube.app.data.models.VideoInfo
import com.freetube.app.data.models.StreamData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShortsUiState(
    val shorts: List<VideoInfo> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Cache of preloaded stream URLs for fast playback
    val preloadedStreams: Map<String, String> = emptyMap()
)

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val youtubeService: YouTubeService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ShortsUiState())
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()
    
    // Cache for preloaded stream URLs
    private val streamCache = mutableMapOf<String, String>()
    
    init {
        loadShorts()
    }
    
    private fun loadShorts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Search for shorts (videos under 60 seconds)
            youtubeService.search("#shorts").fold(
                onSuccess = { page ->
                    val shorts = page.results
                        .filterIsInstance<com.freetube.app.data.models.SearchResult.Video>()
                        .map { it.video }
                        .filter { it.isShort || it.duration < 60 }
                    
                    _uiState.value = _uiState.value.copy(
                        shorts = shorts,
                        isLoading = false
                    )
                    
                    // Start preloading first 3 shorts
                    if (shorts.isNotEmpty()) {
                        preloadShorts(0, 3)
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    /**
     * Preload stream URLs for faster playback
     */
    private fun preloadShorts(startIndex: Int, count: Int) {
        val shorts = _uiState.value.shorts
        viewModelScope.launch {
            for (i in startIndex until minOf(startIndex + count, shorts.size)) {
                val short = shorts[i]
                if (!streamCache.containsKey(short.id)) {
                    val videoUrl = ExtractorHelper.buildVideoUrl(short.id)
                    youtubeService.getStreamData(videoUrl).fold(
                        onSuccess = { streamData ->
                            val streamUrl = getStreamUrl(streamData)
                            if (streamUrl != null) {
                                streamCache[short.id] = streamUrl
                                _uiState.value = _uiState.value.copy(
                                    preloadedStreams = streamCache.toMap()
                                )
                                android.util.Log.d("ShortsViewModel", "Preloaded: ${short.id}")
                            }
                        },
                        onFailure = {
                            android.util.Log.e("ShortsViewModel", "Failed to preload: ${short.id}")
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Get the best stream URL from stream data
     */
    private fun getStreamUrl(streamData: StreamData): String? {
        // Prefer HLS for shorts
        if (!streamData.hlsUrl.isNullOrEmpty()) {
            return streamData.hlsUrl
        }
        
        // Fall back to best progressive stream
        return streamData.videoStreams
            .sortedByDescending { it.bitrate }
            .firstOrNull()?.url
    }
    
    /**
     * Get preloaded stream URL for a video ID
     */
    fun getPreloadedStreamUrl(videoId: String): String? {
        return streamCache[videoId]
    }
    
    fun onPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
        
        // Preload next shorts when scrolling
        preloadShorts(index + 1, 2)
        
        // Load more when near the end
        if (index >= _uiState.value.shorts.size - 3) {
            loadMoreShorts()
        }
    }
    
    private fun loadMoreShorts() {
        // TODO: Implement pagination
    }
    
    fun refresh() {
        streamCache.clear()
        loadShorts()
    }
}
