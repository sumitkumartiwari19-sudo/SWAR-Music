package com.example.ui.playback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Track
import com.example.data.repository.LikedSongsRepository
import com.example.data.repository.PlayerRepository
import com.example.di.AppModule
import com.example.playback.PlaybackMode
import com.example.playback.PlaybackOrigin
import com.example.playback.PlaybackState
import com.example.playback.RepeatMode
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class PlaybackViewModel(
    private val playerRepository: PlayerRepository,
    private val likedSongsRepository: LikedSongsRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerRepository.state
    val exoPlayer: ExoPlayer
        get() = playerRepository.exoPlayer

    private val _isCurrentTrackLiked = MutableStateFlow(false)
    val isCurrentTrackLiked: StateFlow<Boolean> = _isCurrentTrackLiked.asStateFlow()

    init {
        viewModelScope.launch {
            playbackState.collectLatest { state ->
                val track = state.currentTrack
                if (track != null && track.id.isNotBlank()) {
                    likedSongsRepository.isSongLiked(trackId = track.id).collectLatest { liked ->
                        _isCurrentTrackLiked.value = liked
                    }
                } else {
                    _isCurrentTrackLiked.value = false
                }
            }
        }
    }

    fun playTrack(
        track: Track,
        queue: List<Track> = emptyList(),
        origin: PlaybackOrigin = PlaybackOrigin.LIST
    ) {
        playerRepository.playTrack(track, queue, origin)
    }

    fun playYouTubeVideo(
        videoId: String,
        title: String? = null,
        artist: String? = null,
        thumbnailUrl: String? = null,
        origin: PlaybackOrigin = PlaybackOrigin.LIST
    ) {
        playerRepository.playYouTubeVideoId(videoId, title, artist, thumbnailUrl, origin)
    }

    fun togglePlayPause() {
        playerRepository.playPause()
    }

    fun pause() {
        playerRepository.pause()
    }

    fun resume() {
        playerRepository.resume()
    }

    fun seekToFraction(fraction: Float) {
        val duration = playbackState.value.durationMs
        val targetMs = (fraction.coerceIn(0f, 1f) * duration).toLong()
        playerRepository.seekTo(targetMs)
    }

    fun seekToMs(positionMs: Long) {
        playerRepository.seekTo(positionMs)
    }

    fun skipNext() {
        playerRepository.skipToNext()
    }

    fun skipPrevious() {
        playerRepository.skipToPrevious()
    }

    fun toggleRepeat() {
        playerRepository.toggleRepeatMode()
    }

    fun toggleShuffle() {
        playerRepository.toggleShuffle()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        playerRepository.setPlaybackMode(mode)
    }

    fun setVideoQuality(quality: com.example.playback.VideoQuality) {
        playerRepository.setVideoQuality(quality)
    }

    fun retry() {
        playerRepository.retry()
    }

    fun toggleLikeCurrentTrack() {
        val track = playbackState.value.currentTrack ?: return
        viewModelScope.launch {
            likedSongsRepository.toggleLikedSong(track = track)
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlaybackViewModel(
                        playerRepository = AppModule.providePlayerRepository(context),
                        likedSongsRepository = AppModule.provideLikedSongsRepository(context)
                    ) as T
                }
            }
    }
}
