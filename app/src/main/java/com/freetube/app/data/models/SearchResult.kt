package com.freetube.app.data.models

/**
 * Sealed class representing different types of search results
 */
sealed class SearchResult {
    data class Video(val video: VideoInfo) : SearchResult()
    data class Channel(val channel: ChannelInfo) : SearchResult()
    data class Playlist(val playlist: PlaylistInfo) : SearchResult()
}

/**
 * Container for search results with pagination
 */
data class SearchResultPage(
    val results: List<SearchResult>,
    val nextPageToken: String?,
    val estimatedResultCount: Long
)

/**
 * Search filter options
 */
data class SearchFilters(
    val sortBy: SearchSortBy = SearchSortBy.RELEVANCE,
    val uploadDate: UploadDateFilter = UploadDateFilter.ANY,
    val type: SearchTypeFilter = SearchTypeFilter.ALL,
    val duration: DurationFilter = DurationFilter.ANY
)

enum class SearchSortBy {
    RELEVANCE,
    UPLOAD_DATE,
    VIEW_COUNT,
    RATING
}

enum class UploadDateFilter {
    ANY,
    LAST_HOUR,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR
}

enum class SearchTypeFilter {
    ALL,
    VIDEO,
    CHANNEL,
    PLAYLIST
}

enum class DurationFilter {
    ANY,
    SHORT, // Under 4 minutes
    MEDIUM, // 4-20 minutes
    LONG // Over 20 minutes
}

/**
 * Search suggestion
 */
data class SearchSuggestion(
    val query: String,
    val isHistory: Boolean = false
)
