package com.freetube.app.ui.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.extractor.ExtractorHelper
import com.freetube.app.data.extractor.YouTubeService
import com.freetube.app.data.local.SubscriptionDao
import com.freetube.app.data.local.SubscriptionEntity
import com.freetube.app.data.models.ChannelInfo
import com.freetube.app.data.models.VideoInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelUiState(
    val channel: ChannelInfo? = null,
    val videos: List<VideoInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isSubscribed: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val youtubeService: YouTubeService,
    private val subscriptionDao: SubscriptionDao
) : ViewModel() {
    
    private val channelId: String = savedStateHandle.get<String>("channelId") ?: ""
    
    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()
    
    val tabs = listOf("Videos", "Shorts", "Playlists", "About")
    
    init {
        loadChannel()
        checkSubscription()
    }
    
    private fun loadChannel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val channelUrl = ExtractorHelper.buildChannelUrl(channelId)
            
            // Load channel info
            youtubeService.getChannelInfo(channelUrl).fold(
                onSuccess = { channel ->
                    _uiState.value = _uiState.value.copy(channel = channel)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load channel"
                    )
                    return@launch
                }
            )
            
            // Load channel videos
            youtubeService.getChannelVideos(channelUrl).fold(
                onSuccess = { videos ->
                    _uiState.value = _uiState.value.copy(
                        videos = videos,
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
    
    private fun checkSubscription() {
        viewModelScope.launch {
            subscriptionDao.isSubscribedFlow(channelId).collect { isSubscribed ->
                _uiState.value = _uiState.value.copy(isSubscribed = isSubscribed)
            }
        }
    }
    
    fun toggleSubscription() {
        viewModelScope.launch {
            val channel = _uiState.value.channel ?: return@launch
            
            if (_uiState.value.isSubscribed) {
                subscriptionDao.delete(channelId)
            } else {
                subscriptionDao.insert(
                    SubscriptionEntity(
                        channelId = channel.id,
                        channelName = channel.name,
                        avatarUrl = channel.avatarUrl
                    )
                )
            }
        }
    }
    
    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }
}
