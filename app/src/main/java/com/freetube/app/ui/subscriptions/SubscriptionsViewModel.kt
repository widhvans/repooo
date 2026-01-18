package com.freetube.app.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.local.SubscriptionDao
import com.freetube.app.data.local.SubscriptionEntity
import com.freetube.app.data.models.VideoInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionsUiState(
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val videos: List<VideoInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()
    
    init {
        loadSubscriptions()
    }
    
    private fun loadSubscriptions() {
        viewModelScope.launch {
            subscriptionDao.getAll().collect { subscriptions ->
                _uiState.value = _uiState.value.copy(
                    subscriptions = subscriptions
                )
            }
        }
    }
    
    fun refresh() {
        // TODO: Fetch latest videos from subscribed channels
    }
}
