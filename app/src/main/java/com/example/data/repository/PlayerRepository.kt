package com.example.data.repository

import com.example.data.model.Track
import androidx.media3.exoplayer.ExoPlayer
import com.example.playback.PlaybackManager
import com.example.playback.PlaybackMode
import com.example.playback.PlaybackOrigin
import com.example.playback.PlaybackState
import com.example.playback.RepeatMode
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val state: StateFlow<PlaybackState>
    val exoPlayer: ExoPlayer

    fun playTrack(track: Track, queue: List<Track> = emptyList(), origin: PlaybackOrigin = PlaybackOrigin.LIST)
    fun playYouTubeVideoId(
        videoId: String,
        title: String? = null,
        artist: String? = null,
        thumbnailUrl: String? = null,
        origin: PlaybackOrigin = PlaybackOrigin.LIST
    )
    fun playPause()
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun setRepeatMode(mode: RepeatMode)
    fun toggleRepeatMode()
    fun toggleShuffle()
    fun setPlaybackMode(mode: PlaybackMode)
    fun setVideoQuality(quality: com.example.playback.VideoQuality)
    fun retry()
}

class PlayerRepositoryImpl(
    private val playbackManager: PlaybackManager
) : PlayerRepository {

    override val state: StateFlow<PlaybackState> = playbackManager.state
    override val exoPlayer: ExoPlayer
        get() = playbackManager.exoPlayer

    override fun playTrack(track: Track, queue: List<Track>, origin: PlaybackOrigin) {
        playbackManager.playTrack(track, queue, origin)
    }

    override fun playYouTubeVideoId(
        videoId: String,
        title: String?,
        artist: String?,
        thumbnailUrl: String?,
        origin: PlaybackOrigin
    ) {
        playbackManager.playYouTubeVideoId(videoId, title, artist, thumbnailUrl, origin)
    }

    override fun playPause() {
        playbackManager.playPause()
    }

    override fun pause() {
        playbackManager.pause()
    }

    override fun resume() {
        playbackManager.resume()
    }

    override fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    override fun skipToNext() {
        playbackManager.skipToNext()
    }

    override fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }

    override fun setRepeatMode(mode: RepeatMode) {
        playbackManager.setRepeatMode(mode)
    }

    override fun toggleRepeatMode() {
        playbackManager.toggleRepeatMode()
    }

    override fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    override fun setPlaybackMode(mode: PlaybackMode) {
        playbackManager.setPlaybackMode(mode)
    }

    override fun setVideoQuality(quality: com.example.playback.VideoQuality) {
        playbackManager.setVideoQuality(quality)
    }

    override fun retry() {
        playbackManager.retry()
    }
}
