package com.freetube.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.extractor.YouTubeService
import com.freetube.app.data.models.VideoInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val videos: List<VideoInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedCategory: String = "Trending"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val youtubeService: YouTubeService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val categories = listOf(
        "Trending",
        "Music",
        "Gaming",
        "News",
        "Sports",
        "Entertainment",
        "Education",
        "Comedy"
    )
    
    init {
        loadTrendingVideos()
    }
    
    fun loadTrendingVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            youtubeService.getTrendingVideos().fold(
                onSuccess = { videos ->
                    _uiState.value = _uiState.value.copy(
                        videos = videos,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load videos"
                    )
                }
            )
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            youtubeService.getTrendingVideos().fold(
                onSuccess = { videos ->
                    _uiState.value = _uiState.value.copy(
                        videos = videos,
                        isRefreshing = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh"
                    )
                }
            )
        }
    }
    
    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        // For now, just reload trending - in a full implementation,
        // you'd filter by category using YouTube's category API
        loadTrendingVideos()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
