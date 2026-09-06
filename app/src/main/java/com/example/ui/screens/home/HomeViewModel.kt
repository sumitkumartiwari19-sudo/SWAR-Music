package com.example.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DownloadedTrackEntity
import com.example.data.model.CuratedTracks
import com.example.data.model.CustomPlaylist
import com.example.data.model.ImportedYouTubePlaylist
import com.example.data.model.LikedSong
import com.example.data.model.RecentlyPlayedItem
import com.example.data.model.Track
import com.example.data.preferences.ThemeMode
import com.example.di.AppModule
import com.example.playback.extractor.MusicContentFilter
import com.example.playback.extractor.YouTubeSearchService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "HomeViewModel"

        // Curated Indian queries for backing recommendations
        private val QUICK_PICK_QUERIES = listOf(
            "bollywood hits 2026",
            "bollywood romantic songs",
            "hindi lofi songs",
            "arijit singh best songs"
        )

        private val TRENDING_QUERIES = listOf(
            "trending hindi songs",
            "punjabi hits 2026",
            "bollywood party songs",
            "top indian songs 2026"
        )

        private val NEW_RELEASES_QUERIES = listOf(
            "regional hits telugu tamil",
            "indian classical instrumental raga",
            "latest hindi songs 2026"
        )
    }

    init {
        AppModule.init(application)
    }

    private val themePreferences = AppModule.provideThemePreferences(application)
    private val settingsPreferences = AppModule.provideSettingsPreferences(application)
    private val likedSongsRepository = AppModule.provideLikedSongsRepository(application)
    private val playlistsRepository = AppModule.providePlaylistsRepository(application)
    private val historyAndQueueRepository = AppModule.provideHistoryAndQueueRepository(application)
    private val downloadsRepository = AppModule.provideDownloadsRepository(application)
    private val playlistExtractor = AppModule.providePlaylistExtractor()
    private val searchService = YouTubeSearchService()

    private val _isImportingPlaylist = MutableStateFlow(false)
    val isImportingPlaylist: StateFlow<Boolean> = _isImportingPlaylist.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    // Library active tab index (0=Playlists, 1=Downloads, 2=Liked, 3=History) - preserved across back-navigation
    private val _selectedLibraryTab = MutableStateFlow(0)
    val selectedLibraryTab: StateFlow<Int> = _selectedLibraryTab.asStateFlow()

    fun setSelectedLibraryTab(tabIndex: Int) {
        _selectedLibraryTab.value = tabIndex.coerceIn(0, 3)
    }

    // India-optimized dynamic sections state
    private val _rawQuickPicks = MutableStateFlow<List<Track>>(CuratedTracks.quickPicks)
    private val _rawTrendingNow = MutableStateFlow<List<Track>>(CuratedTracks.trendingNow)
    private val _rawNewReleases = MutableStateFlow<List<Track>>(CuratedTracks.newReleasesAndRegional)
    private val _isLoadingRecommendations = MutableStateFlow<Boolean>(false)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val likedSongs: StateFlow<List<LikedSong>> = likedSongsRepository.getLikedSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customPlaylists: StateFlow<List<CustomPlaylist>> = playlistsRepository.getCustomPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val importedPlaylists: StateFlow<List<ImportedYouTubePlaylist>> = playlistsRepository.getImportedPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentlyPlayed: StateFlow<List<RecentlyPlayedItem>> = historyAndQueueRepository.getRecentlyPlayed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Blends personalized listening history (liked songs, recently played) with
     * India curated cold-start tracks when personalizedRecommendations is ON.
     */
    val quickPicks: StateFlow<List<Track>> = combine(
        _rawQuickPicks,
        recentlyPlayed,
        likedSongs,
        settingsPreferences.personalizedRecommendations
    ) { curated, recent, liked, personalizedEnabled ->
        if (!personalizedEnabled) {
            MusicContentFilter.filterAndRankSongs(curated)
        } else {
            val personalizedTracks = mutableListOf<Track>()
            // Add up to 3 recently played tracks
            recent.take(3).forEach { item ->
                personalizedTracks.add(item.track)
            }
            // Add up to 2 liked songs not already present
            liked.take(2).forEach { item ->
                if (personalizedTracks.none { it.id == item.track.id }) {
                    personalizedTracks.add(item.track)
                }
            }
            // Combine with India curated recommendations
            val combined = (personalizedTracks + curated).distinctBy { it.id }
            MusicContentFilter.filterAndRankSongs(combined)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CuratedTracks.quickPicks
    )

    val trendingNow: StateFlow<List<Track>> = _rawTrendingNow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CuratedTracks.trendingNow
        )

    val newReleases: StateFlow<List<Track>> = _rawNewReleases
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CuratedTracks.newReleasesAndRegional
        )

    val downloadedTracks: StateFlow<List<DownloadedTrackEntity>> = downloadsRepository.getAllDownloadedTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedDownloads: StateFlow<List<DownloadedTrackEntity>> = downloadsRepository.getCompletedDownloadedTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalStorageBytes: StateFlow<Long> = downloadsRepository.getTotalStorageSizeBytes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    init {
        loadIndiaRecommendations()
    }

    /**
     * Loads live India-optimized recommendations using rotating queries from YouTube/NewPipe extractor.
     */
    fun loadIndiaRecommendations(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoadingRecommendations.value = true

                val quickPickQuery = QUICK_PICK_QUERIES[Random.nextInt(QUICK_PICK_QUERIES.size)]
                val trendingQuery = TRENDING_QUERIES[Random.nextInt(TRENDING_QUERIES.size)]
                val newReleaseQuery = NEW_RELEASES_QUERIES[Random.nextInt(NEW_RELEASES_QUERIES.size)]

                Log.d(TAG, "Loading India recommendations: QuickPicks='$quickPickQuery', Trending='$trendingQuery', NewReleases='$newReleaseQuery'")

                val qpResult = searchService.searchTracks(quickPickQuery)
                qpResult.onSuccess { tracks ->
                    val songs = MusicContentFilter.filterAndRankSongs(tracks)
                    if (songs.isNotEmpty()) {
                        _rawQuickPicks.value = songs
                    }
                }

                val trendingResult = searchService.searchTracks(trendingQuery)
                trendingResult.onSuccess { tracks ->
                    val songs = MusicContentFilter.filterAndRankSongs(tracks)
                    if (songs.isNotEmpty()) {
                        _rawTrendingNow.value = songs
                    }
                }

                val nrResult = searchService.searchTracks(newReleaseQuery)
                nrResult.onSuccess { tracks ->
                    val songs = MusicContentFilter.filterAndRankSongs(tracks)
                    if (songs.isNotEmpty()) {
                        _rawNewReleases.value = songs
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading live India recommendations: ${e.message}")
            } finally {
                _isLoadingRecommendations.value = false
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun toggleLikedSong(track: Track) {
        if (track.id.isBlank()) return
        viewModelScope.launch {
            likedSongsRepository.toggleLikedSong(track = track)
        }
    }

    fun recordTrackPlayed(track: Track) {
        if (track.id.isBlank()) return
        viewModelScope.launch {
            historyAndQueueRepository.recordTrackPlayed(track = track)
        }
    }

    fun createPlaylist(name: String, description: String = "", initialTracks: List<Track> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = playlistsRepository.createCustomPlaylist(name = name, description = description)
            result.onSuccess { playlist ->
                if (initialTracks.isNotEmpty()) {
                    playlistsRepository.updatePlaylistTracks(playlistId = playlist.id, tracks = initialTracks)
                }
            }
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        if (playlistId.isBlank() || track.id.isBlank()) return
        viewModelScope.launch {
            playlistsRepository.addTrackToPlaylist(playlistId = playlistId, track = track)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        if (playlistId.isBlank() || trackId.isBlank()) return
        viewModelScope.launch {
            playlistsRepository.removeTrackFromPlaylist(playlistId = playlistId, trackId = trackId)
        }
    }

    fun reorderCustomPlaylist(playlistId: String, reorderedTracks: List<Track>) {
        if (playlistId.isBlank()) return
        viewModelScope.launch {
            playlistsRepository.updatePlaylistTracks(playlistId = playlistId, tracks = reorderedTracks)
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId.isBlank()) return
        viewModelScope.launch {
            playlistsRepository.deleteCustomPlaylist(playlistId = playlistId)
        }
    }

    fun importYouTubePlaylist(urlOrId: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isImportingPlaylist.value = true
            _importError.value = null
            val result = playlistExtractor.extractPlaylist(urlOrId)
            result.fold(
                onSuccess = { importedPlaylist ->
                    val saveResult = playlistsRepository.saveImportedPlaylist(playlist = importedPlaylist)
                    _isImportingPlaylist.value = false
                    if (saveResult.isSuccess) {
                        onComplete?.invoke(true)
                    } else {
                        _importError.value = "Failed to save playlist: ${saveResult.exceptionOrNull()?.message}"
                        onComplete?.invoke(false)
                    }
                },
                onFailure = { error ->
                    _isImportingPlaylist.value = false
                    _importError.value = "Extraction failed: ${error.localizedMessage ?: "Invalid URL or network issue"}"
                    onComplete?.invoke(false)
                }
            )
        }
    }

    fun deleteImportedPlaylist(playlistId: String) {
        if (playlistId.isBlank()) return
        viewModelScope.launch {
            playlistsRepository.deleteImportedPlaylist(playlistId = playlistId)
        }
    }

    fun clearImportError() {
        _importError.value = null
    }

    // Download controls
    fun downloadTrack(track: Track) {
        downloadsRepository.downloadTrack(track)
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            downloadsRepository.deleteDownloadedTrack(trackId)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadsRepository.deleteAllDownloadedTracks()
        }
    }

    fun formatStorageSize(bytes: Long): String {
        return downloadsRepository.formatStorageSize(bytes)
    }
}
