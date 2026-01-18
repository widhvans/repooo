package com.freetube.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.auth.AuthManager
import com.freetube.app.data.local.DownloadDao
import com.freetube.app.data.local.WatchHistoryDao
import com.freetube.app.data.local.WatchLaterDao
import com.freetube.app.data.models.DownloadItem
import com.freetube.app.data.models.WatchHistoryItem
import com.freetube.app.data.models.WatchLaterItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val recentHistory: List<WatchHistoryItem> = emptyList(),
    val watchLaterCount: Int = 0,
    val downloadsCount: Int = 0,
    val historyCount: Int = 0,
    val isSignedIn: Boolean = false,
    val userName: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    private val watchLaterDao: WatchLaterDao,
    private val downloadDao: DownloadDao,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        // Check auth state
        viewModelScope.launch {
            authManager.isSignedIn.collect { isSignedIn ->
                _uiState.value = _uiState.value.copy(isSignedIn = isSignedIn)
            }
        }
        
        viewModelScope.launch {
            authManager.userName.collect { name ->
                _uiState.value = _uiState.value.copy(userName = name)
            }
        }
        
        viewModelScope.launch {
            // Recent history
            watchHistoryDao.getRecentHistory(5).collect { history ->
                _uiState.value = _uiState.value.copy(
                    recentHistory = history,
                    historyCount = watchHistoryDao.getCount()
                )
            }
        }
        
        viewModelScope.launch {
            watchLaterDao.getAll().collect { items ->
                _uiState.value = _uiState.value.copy(
                    watchLaterCount = items.size
                )
            }
        }
        
        viewModelScope.launch {
            downloadDao.getAll().collect { downloads ->
                _uiState.value = _uiState.value.copy(
                    downloadsCount = downloads.size
                )
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }
}

// Watch History Screen
data class HistoryUiState(
    val history: List<WatchHistoryItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            watchHistoryDao.getAllHistory().collect { history ->
                _uiState.value = HistoryUiState(
                    history = history,
                    isLoading = false
                )
            }
        }
    }
    
    fun deleteItem(videoId: String) {
        viewModelScope.launch {
            watchHistoryDao.deleteById(videoId)
        }
    }
    
    fun clearAll() {
        viewModelScope.launch {
            watchHistoryDao.clearAll()
        }
    }
}

// Watch Later Screen
data class WatchLaterUiState(
    val items: List<WatchLaterItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WatchLaterViewModel @Inject constructor(
    private val watchLaterDao: WatchLaterDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WatchLaterUiState())
    val uiState: StateFlow<WatchLaterUiState> = _uiState.asStateFlow()
    
    init {
        loadItems()
    }
    
    private fun loadItems() {
        viewModelScope.launch {
            watchLaterDao.getAll().collect { items ->
                _uiState.value = WatchLaterUiState(
                    items = items,
                    isLoading = false
                )
            }
        }
    }
    
    fun deleteItem(videoId: String) {
        viewModelScope.launch {
            watchLaterDao.deleteById(videoId)
        }
    }
    
    fun clearAll() {
        viewModelScope.launch {
            watchLaterDao.clearAll()
        }
    }
}

// Downloads Screen
data class DownloadsUiState(
    val downloads: List<DownloadItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadDao: DownloadDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()
    
    init {
        loadDownloads()
    }
    
    private fun loadDownloads() {
        viewModelScope.launch {
            downloadDao.getAll().collect { downloads ->
                _uiState.value = DownloadsUiState(
                    downloads = downloads,
                    isLoading = false
                )
            }
        }
    }
    
    fun deleteItem(videoId: String) {
        viewModelScope.launch {
            downloadDao.deleteById(videoId)
        }
    }
}
