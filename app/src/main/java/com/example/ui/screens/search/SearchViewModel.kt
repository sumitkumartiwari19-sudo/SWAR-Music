package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CuratedTracks
import com.example.data.model.Track
import com.example.playback.extractor.MusicContentFilter
import com.example.playback.extractor.YouTubeSearchService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedCategoryIndex: Int = 0,
    val isSearching: Boolean = false,
    val searchResults: List<Track> = CuratedTracks.allCurated,
    val searchError: String? = null,
    val isOnlineResult: Boolean = false
)

class SearchViewModel(
    private val searchService: YouTubeSearchService = YouTubeSearchService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    val categories = listOf(
        "All",
        "Bollywood Hits",
        "Trending Hindi",
        "Punjabi",
        "Romantic & Lo-Fi",
        "Classical Ragas",
        "Regional Hits"
    )

    private val categoryQueries = listOf(
        "",
        "bollywood hits 2026",
        "trending hindi songs",
        "punjabi hits 2026",
        "bollywood romantic songs lofi",
        "indian classical instrumental raga",
        "regional hits telugu tamil"
    )

    init {
        updateFilteredCuratedTracks()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            updateFilteredCuratedTracks()
            return
        }

        // Live search with debounce for typing
        searchJob = viewModelScope.launch {
            delay(400) // Debounce typing
            performLiveSearch(newQuery)
        }
    }

    fun onCategorySelect(index: Int) {
        _uiState.update { it.copy(selectedCategoryIndex = index) }
        if (_uiState.value.query.isBlank()) {
            val catQuery = categoryQueries.getOrElse(index) { "" }
            if (catQuery.isNotBlank()) {
                performLiveSearch(catQuery)
            } else {
                updateFilteredCuratedTracks()
            }
        } else {
            onQueryChange(_uiState.value.query)
        }
    }

    fun performLiveSearch(query: String? = null) {
        val targetQuery = query ?: _uiState.value.query
        if (targetQuery.isBlank()) {
            updateFilteredCuratedTracks()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            val result = searchService.searchTracks(targetQuery)
            result.fold(
                onSuccess = { tracks ->
                    val songOnlyTracks = MusicContentFilter.filterAndRankSongs(tracks, targetQuery)
                    if (songOnlyTracks.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                searchResults = songOnlyTracks,
                                searchError = null,
                                isOnlineResult = true
                            )
                        }
                    } else {
                        val localFiltered = filterLocalCurated(targetQuery)
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                searchResults = localFiltered,
                                searchError = "No online song tracks found for \"$targetQuery\". Showing local matches.",
                                isOnlineResult = false
                            )
                        }
                    }
                },
                onFailure = { error ->
                    val localFiltered = filterLocalCurated(targetQuery)
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchResults = if (localFiltered.isNotEmpty()) localFiltered else CuratedTracks.allCurated,
                            searchError = "Search notice: ${error.localizedMessage ?: error.javaClass.simpleName}",
                            isOnlineResult = false
                        )
                    }
                }
            )
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(query = "", searchError = null) }
        searchJob?.cancel()
        updateFilteredCuratedTracks()
    }

    private fun updateFilteredCuratedTracks() {
        val all = CuratedTracks.allCurated
        val categoryFiltered = when (_uiState.value.selectedCategoryIndex) {
            1 -> all.filter { it.title.contains("Kesariya", true) || it.title.contains("Chaleya", true) || it.title.contains("Apna Bana Le", true) || it.title.contains("Tum Hi Ho", true) }
            2 -> CuratedTracks.trendingNow
            3 -> all.filter { it.artist.contains("Karan Aujla", true) || it.artist.contains("Diljit", true) || it.artist.contains("Ali Sethi", true) }
            4 -> all.filter { it.title.contains("Lo-Fi", true) || it.title.contains("Heeriye", true) || it.title.contains("Kahani Suno", true) }
            5 -> all.filter { it.title.contains("Raga", true) || it.title.contains("Raag", true) || it.artist.contains("Pandit", true) || it.artist.contains("Amjad", true) }
            6 -> CuratedTracks.newReleasesAndRegional
            else -> all
        }

        _uiState.update {
            it.copy(
                isSearching = false,
                searchResults = MusicContentFilter.filterAndRankSongs(if (categoryFiltered.isNotEmpty()) categoryFiltered else all),
                searchError = null,
                isOnlineResult = false
            )
        }
    }

    private fun filterLocalCurated(query: String): List<Track> {
        val filtered = CuratedTracks.allCurated.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true)
        }
        return MusicContentFilter.filterAndRankSongs(filtered, query)
    }
}
