package com.freetube.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.auth.AuthManager
import com.freetube.app.data.extractor.YouTubeService
import com.freetube.app.data.models.VideoInfo
import com.freetube.app.data.models.SearchFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val videos: List<VideoInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedCategory: String = "For You",
    val isSignedIn: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val youtubeService: YouTubeService,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // Category to search query mapping
    // When signed in: "For You" shows recommended, otherwise trending
    private val categoryQueries = mapOf(
        "For You" to "",
        "Trending" to "trending",
        "Music" to "music songs",
        "Gaming" to "gaming gameplay",
        "News" to "news today",
        "Sports" to "sports highlights",
        "Entertainment" to "entertainment",
        "Comedy" to "comedy funny"
    )
    
    val categories = categoryQueries.keys.toList()
    
    init {
        checkAuthAndLoadVideos()
    }
    
    private fun checkAuthAndLoadVideos() {
        viewModelScope.launch {
            // Check if signed in
            val isSignedIn = authManager.isSignedIn.first()
            _uiState.value = _uiState.value.copy(isSignedIn = isSignedIn)
            
            // Load videos based on auth state
            if (isSignedIn) {
                loadSubscriptionFeed()
            } else {
                loadVideosForCategory("Trending")
            }
        }
        
        // Also observe auth changes
        viewModelScope.launch {
            authManager.isSignedIn.collect { isSignedIn ->
                _uiState.value = _uiState.value.copy(isSignedIn = isSignedIn)
                if (isSignedIn && _uiState.value.selectedCategory == "For You") {
                    loadSubscriptionFeed()
                }
            }
        }
    }
    
    private fun loadSubscriptionFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Get user's subscriptions feed
            youtubeService.getSubscriptionFeed().fold(
                onSuccess = { videos ->
                    if (videos.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            videos = videos,
                            isLoading = false,
                            error = null,
                            selectedCategory = "For You"
                        )
                    } else {
                        // Fallback to trending if no subscription videos
                        loadTrendingVideos()
                    }
                },
                onFailure = { e ->
                    android.util.Log.e("HomeViewModel", "Failed to load subscription feed: ${e.message}")
                    // Fallback to trending on error
                    loadTrendingVideos()
                }
            )
        }
    }
    
    fun loadTrendingVideos() {
        loadVideosForCategory(_uiState.value.selectedCategory)
    }
    
    private fun loadVideosForCategory(category: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val query = categoryQueries[category] ?: ""
            
            // For You = subscription feed if signed in, trending if not
            when {
                category == "For You" && _uiState.value.isSignedIn -> {
                    loadSubscriptionFeed()
                    return@launch
                }
                category == "For You" || category == "Trending" || query.isEmpty() -> {
                    // Use trending API
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
                else -> {
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
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            val category = _uiState.value.selectedCategory
            
            if (category == "For You" && _uiState.value.isSignedIn) {
                youtubeService.getSubscriptionFeed().fold(
                    onSuccess = { videos ->
                        _uiState.value = _uiState.value.copy(
                            videos = if (videos.isNotEmpty()) videos else _uiState.value.videos,
                            isRefreshing = false,
                            error = null
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = e.message
                        )
                    }
                )
            } else {
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
