package com.freetube.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.extractor.YouTubeService
import com.freetube.app.data.models.VideoInfo
import com.freetube.app.data.models.SearchFilters
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
    
    // Category to search query mapping
    private val categoryQueries = mapOf(
        "Trending" to "",
        "Music" to "music songs",
        "Gaming" to "gaming gameplay",
        "News" to "news today",
        "Sports" to "sports highlights",
        "Entertainment" to "entertainment",
        "Education" to "educational",
        "Comedy" to "comedy funny"
    )
    
    val categories = categoryQueries.keys.toList()
    
    init {
        loadVideosForCategory("Trending")
    }
    
    fun loadTrendingVideos() {
        loadVideosForCategory(_uiState.value.selectedCategory)
    }
    
    private fun loadVideosForCategory(category: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val query = categoryQueries[category] ?: ""
            
            if (category == "Trending" || query.isEmpty()) {
                // Use trending API for main tab
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
            } else {
                // Use search for category tabs
                youtubeService.search(query, SearchFilters()).fold(
                    onSuccess = { resultPage ->
                        val videos = resultPage.results
                            .filterIsInstance<com.freetube.app.data.models.SearchResult.Video>()
                            .map { it.toVideoInfo() }
                        
                        _uiState.value = _uiState.value.copy(
                            videos = videos,
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load $category videos"
                        )
                    }
                )
            }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            val category = _uiState.value.selectedCategory
            val query = categoryQueries[category] ?: ""
            
            if (category == "Trending" || query.isEmpty()) {
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
            } else {
                youtubeService.search(query, SearchFilters()).fold(
                    onSuccess = { resultPage ->
                        val videos = resultPage.results
                            .filterIsInstance<com.freetube.app.data.models.SearchResult.Video>()
                            .map { it.toVideoInfo() }
                        
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
    }
    
    fun selectCategory(category: String) {
        if (category != _uiState.value.selectedCategory) {
            _uiState.value = _uiState.value.copy(selectedCategory = category)
            loadVideosForCategory(category)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
