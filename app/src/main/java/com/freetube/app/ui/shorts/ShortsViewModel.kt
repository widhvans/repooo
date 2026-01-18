package com.freetube.app.ui.shorts

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

data class ShortsUiState(
    val shorts: List<VideoInfo> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val youtubeService: YouTubeService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ShortsUiState())
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()
    
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
    
    fun onPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
        
        // Load more when near the end
        if (index >= _uiState.value.shorts.size - 3) {
            loadMoreShorts()
        }
    }
    
    private fun loadMoreShorts() {
        // TODO: Implement pagination
    }
    
    fun refresh() {
        loadShorts()
    }
}
