package com.freetube.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.extractor.YouTubeService
import com.freetube.app.data.local.SearchHistoryDao
import com.freetube.app.data.local.SearchHistoryEntity
import com.freetube.app.data.models.SearchResult
import com.freetube.app.data.models.SearchFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val filters: SearchFilters = SearchFilters()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youtubeService: YouTubeService,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    
    init {
        loadSearchHistory()
    }
    
    private fun loadSearchHistory() {
        viewModelScope.launch {
            searchHistoryDao.getRecent(10).collect { history ->
                _uiState.value = _uiState.value.copy(
                    searchHistory = history.map { it.query }
                )
            }
        }
    }
    
    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        
        if (query.isNotEmpty()) {
            loadSuggestions(query)
        } else {
            _uiState.value = _uiState.value.copy(suggestions = emptyList())
        }
    }
    
    private fun loadSuggestions(query: String) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(300) // Debounce
            
            youtubeService.getSearchSuggestions(query).fold(
                onSuccess = { suggestions ->
                    _uiState.value = _uiState.value.copy(suggestions = suggestions)
                },
                onFailure = {
                    // Ignore suggestion errors
                }
            )
        }
    }
    
    fun search(query: String = _uiState.value.query) {
        if (query.isEmpty()) return
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                query = query,
                isSearching = true,
                error = null,
                suggestions = emptyList()
            )
            
            // Save to history
            searchHistoryDao.insert(SearchHistoryEntity(query = query))
            
            youtubeService.search(query, _uiState.value.filters).fold(
                onSuccess = { page ->
                    _uiState.value = _uiState.value.copy(
                        results = page.results,
                        isSearching = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        error = e.message ?: "Search failed"
                    )
                }
            )
        }
    }
    
    fun clearQuery() {
        _uiState.value = _uiState.value.copy(
            query = "",
            suggestions = emptyList(),
            results = emptyList()
        )
    }
    
    fun deleteFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryDao.delete(query)
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryDao.clearAll()
        }
    }
}
