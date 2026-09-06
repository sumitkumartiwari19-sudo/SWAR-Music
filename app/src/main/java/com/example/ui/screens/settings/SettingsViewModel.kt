package com.example.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppSettings
import com.example.data.preferences.AudioQuality
import com.example.data.preferences.DownloadAudioQuality
import com.example.data.preferences.FontSizeScale
import com.example.data.preferences.NotificationControlsStyle
import com.example.data.preferences.ThemeMode
import com.example.di.AppModule
import com.example.playback.VideoQuality
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val settingsPreferences = AppModule.provideSettingsPreferences(application)
    private val downloadsRepository = AppModule.provideDownloadsRepository(application)
    private val historyAndQueueRepository = AppModule.provideHistoryAndQueueRepository(application)
    private val playlistsRepository = AppModule.providePlaylistsRepository(application)

    val settings: StateFlow<AppSettings> = settingsPreferences.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    val totalStorageBytes: StateFlow<Long> = downloadsRepository.getTotalStorageSizeBytes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsPreferences.setThemeMode(mode)
        }
    }

    fun setFontSizeScale(scale: FontSizeScale) {
        viewModelScope.launch {
            settingsPreferences.setFontSizeScale(scale)
        }
    }

    fun setHighContrastMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setHighContrastMode(enabled)
        }
    }

    fun setDefaultAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsPreferences.setDefaultAudioQuality(quality)
        }
    }

    fun setDefaultVideoQuality(quality: VideoQuality) {
        viewModelScope.launch {
            settingsPreferences.setDefaultVideoQuality(quality)
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setGaplessPlayback(enabled)
        }
    }

    fun setCrossfadeDurationSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsPreferences.setCrossfadeDurationSeconds(seconds)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            settingsPreferences.setPlaybackSpeed(speed)
        }
    }

    fun setAudioNormalization(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setAudioNormalization(enabled)
        }
    }

    fun setAutoplaySimilar(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setAutoplaySimilar(enabled)
        }
    }

    fun setResumePlaybackOnOpen(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setResumePlaybackOnOpen(enabled)
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setSkipSilence(enabled)
        }
    }

    fun setDownloadAudioQuality(quality: DownloadAudioQuality) {
        viewModelScope.launch {
            settingsPreferences.setDownloadAudioQuality(quality)
        }
    }

    fun setDownloadOnlyOnWifi(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setDownloadOnlyOnWifi(enabled)
        }
    }

    fun setMobileStreamingQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsPreferences.setMobileStreamingQuality(quality)
        }
    }

    fun setWifiStreamingQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsPreferences.setWifiStreamingQuality(quality)
        }
    }

    fun setNotificationControlsStyle(style: NotificationControlsStyle) {
        viewModelScope.launch {
            settingsPreferences.setNotificationControlsStyle(style)
        }
    }

    fun setPersonalizedRecommendations(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setPersonalizedRecommendations(enabled)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadsRepository.deleteAllDownloadedTracks()
            _userMessage.emit("All downloaded files cleared")
        }
    }

    fun clearRecentlyPlayedHistory() {
        viewModelScope.launch {
            historyAndQueueRepository.clearRecentlyPlayed()
            _userMessage.emit("Listening history cleared successfully")
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            _userMessage.emit("Search history cleared")
        }
    }

    fun resetAllPreferences() {
        viewModelScope.launch {
            settingsPreferences.resetAllToDefaults()
            _userMessage.emit("All settings reset to default values")
        }
    }

    fun formatStorageSize(bytes: Long): String {
        return downloadsRepository.formatStorageSize(bytes)
    }

    fun exportPlaylistsToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val playlists = playlistsRepository.getCustomPlaylists().first()
                val rootJson = JSONObject().apply {
                    put("app", "SWAR Music")
                    put("exportedAt", System.currentTimeMillis())
                    put("version", 1)
                    val array = JSONArray()
                    playlists.forEach { playlist ->
                        val pObj = JSONObject().apply {
                            put("id", playlist.id)
                            put("name", playlist.name)
                            put("description", playlist.description)
                            put("trackCount", playlist.tracks.size)
                            val tracksArray = JSONArray()
                            playlist.tracks.forEach { track ->
                                val tObj = JSONObject().apply {
                                    put("id", track.id)
                                    put("title", track.title)
                                    put("artist", track.artist)
                                    put("durationSeconds", track.durationSeconds)
                                    put("thumbnailUrl", track.thumbnailUrl)
                                }
                                tracksArray.put(tObj)
                            }
                            put("tracks", tracksArray)
                        }
                        array.put(pObj)
                    }
                    put("playlists", array)
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(rootJson.toString(2))
                    }
                }
                _userMessage.emit("Successfully exported ${playlists.size} playlist(s)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export playlists: ${e.message}", e)
                _userMessage.emit("Export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
}
