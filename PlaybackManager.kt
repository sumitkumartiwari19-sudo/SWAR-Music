package com.example.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.dao.QuickSkipDao
import com.example.data.local.entity.QuickSkipEntity
import com.example.data.model.PlaybackQueueState
import com.example.data.model.Track
import com.example.data.repository.AuthRepository
import com.example.data.repository.DownloadsRepository
import com.example.data.repository.HistoryAndQueueRepository
import com.example.playback.extractor.AudioStreamResolver
import com.example.playback.extractor.MusicKeywordExtractor
import com.example.playback.radio.RelatedTrackGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
class PlaybackManager(
    private val context: Context,
    private val streamResolver: AudioStreamResolver = AudioStreamResolver(),
    private val authRepository: AuthRepository? = null,
    private val historyAndQueueRepository: HistoryAndQueueRepository? = null,
    private val downloadsRepository: DownloadsRepository? = null,
    private val quickSkipDao: QuickSkipDao? = null,
    private val relatedTrackGenerator: RelatedTrackGenerator = RelatedTrackGenerator(quickSkipDao = quickSkipDao)
) {
    companion object {
        private const val TAG = "PlaybackManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var resolveJob: Job? = null
    private var nextRadioJob: Job? = null

    // Real play history stack maintained strictly for SEARCH-origin radio sessions
    private val searchHistoryStack = mutableListOf<Track>()
    private var searchHistoryIndex = -1
    private var isGeneratingRadioTrack = false

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(playerListener)
            }
    }

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _state.update { it.copy(isBuffering = true, errorMessage = null) }
                }
                Player.STATE_READY -> {
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _state.value.durationMs
                    _state.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = exoPlayer.isPlaying,
                            durationMs = duration,
                            currentPositionMs = exoPlayer.currentPosition,
                            errorMessage = null
                        )
                    }
                    startProgressTracking()
                }
                Player.STATE_ENDED -> {
                    _state.update { it.copy(isPlaying = false, isBuffering = false) }
                    stopProgressTracking()
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _state.update { it.copy(isBuffering = false) }
                    stopProgressTracking()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressTracking()
                ensureForegroundService()
            } else {
                stopProgressTracking()
            }
            syncQueueToCloud()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "ExoPlayer error: ${error.message}", error)
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Playback error: ${error.localizedMessage ?: "Failed to play audio stream"}"
                )
            }
        }
    }

    private fun ensureForegroundService() {
        try {
            val intent = Intent(context, SwarPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start playback service: ${e.message}")
        }
    }

    private fun checkAndRecordQuickSkip(previousTrack: Track?) {
        if (previousTrack == null) return
        val posMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        // Strictly between 2 and 10 seconds (2000ms <= posMs <= 10000ms)
        if (posMs in 2000L..10000L) {
            val keywords = MusicKeywordExtractor.extractKeywords(previousTrack.title, previousTrack.artist).joinToString(",")
            Log.i(
                TAG,
                "[QuickSkip] Implicit negative signal: Track '${previousTrack.title}' by '${previousTrack.artist}' skipped after ${posMs / 1000.0}s (keywords=[$keywords])"
            )
            val dao = quickSkipDao
            if (dao != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        dao.insertQuickSkip(
                            QuickSkipEntity(
                                trackId = previousTrack.id,
                                title = previousTrack.title,
                                artist = previousTrack.artist,
                                keywords = keywords,
                                listenDurationMs = posMs,
                                skippedAt = System.currentTimeMillis()
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error recording quick-skip: ${e.message}")
                    }
                }
            }
        }
    }

    fun playTrack(
        track: Track,
        newQueue: List<Track> = emptyList(),
        origin: PlaybackOrigin = PlaybackOrigin.LIST
    ) {
        Log.d(TAG, "[AudioTrace:Step-A] playTrack called for id='${track.id}', title='${track.title}', artist='${track.artist}', origin=$origin")
        checkAndRecordQuickSkip(_state.value.currentTrack)

        val updatedQueue: List<Track>
        val targetIndex: Int

        if (origin == PlaybackOrigin.SEARCH) {
            // SEARCH-origin session: Initialize real play history stack with this tapped search track
            searchHistoryStack.clear()
            searchHistoryStack.add(track)
            searchHistoryIndex = 0
            updatedQueue = searchHistoryStack.toList()
            targetIndex = 0
        } else {
            // LIST-origin session (playlists, downloads, home queue): strictly keep list order
            searchHistoryStack.clear()
            searchHistoryIndex = -1
            updatedQueue = when {
                newQueue.isNotEmpty() -> newQueue
                _state.value.queue.none { it.id == track.id } -> _state.value.queue + track
                else -> _state.value.queue
            }
            targetIndex = updatedQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        }

        _state.update {
            it.copy(
                currentTrack = track,
                queue = updatedQueue,
                currentIndex = targetIndex,
                playbackOrigin = origin,
                isBuffering = true,
                errorMessage = null,
                currentPositionMs = 0L,
                durationMs = track.durationSeconds * 1000L,
                playbackMode = PlaybackMode.AUDIO,
                resolvedVideoStreamUrl = null
            )
        }

        resolveAndPlay(track)
    }

    fun playYouTubeVideoId(
        videoId: String,
        title: String? = null,
        artist: String? = null,
        thumbnailUrl: String? = null,
        origin: PlaybackOrigin = PlaybackOrigin.LIST
    ) {
        Log.d(TAG, "[AudioTrace:Step-A] playYouTubeVideoId: videoId='$videoId', title='$title', origin=$origin")
        val track = Track(
            id = videoId,
            title = title ?: "YouTube Audio ($videoId)",
            artist = artist ?: "SWAR Streaming",
            thumbnailUrl = thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            addedAt = System.currentTimeMillis()
        )
        playTrack(track, origin = origin)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        if (_state.value.playbackMode == mode) return
        val currentTrack = _state.value.currentTrack ?: return
        val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val wasPlaying = exoPlayer.isPlaying

        Log.d(TAG, "Switching playback mode to $mode at position $currentPos ms (wasPlaying=$wasPlaying)")
        _state.update { it.copy(playbackMode = mode) }

        if (mode == PlaybackMode.VIDEO) {
            val videoUrl = _state.value.resolvedVideoStreamUrl
            if (videoUrl != null) {
                playStreamUrl(videoUrl, currentTrack, currentPos)
            } else {
                resolveAndPlay(currentTrack, startPositionMs = currentPos, preferVideo = true)
            }
        } else {
            val audioUrl = _state.value.resolvedStreamUrl
            if (audioUrl != null) {
                playStreamUrl(audioUrl, currentTrack, currentPos)
            } else {
                resolveAndPlay(currentTrack, startPositionMs = currentPos, preferVideo = false)
            }
        }
    }

    private fun resolveAndPlay(track: Track, startPositionMs: Long = 0L, preferVideo: Boolean = false) {
        resolveJob?.cancel()
        resolveJob = scope.launch {
            Log.d(TAG, "[PlaybackDecision:START] Resolving track id='${track.id}', title='${track.title}' (preferVideo=$preferVideo)")
            _state.update { it.copy(isBuffering = true, errorMessage = null) }

            // 1. Check if track is downloaded locally FIRST for instant playback (when in Audio mode)
            if (!preferVideo) {
                val localFile = try {
                    downloadsRepository?.getLocalTrackFile(track.id)
                } catch (e: Exception) {
                    Log.w(TAG, "[PlaybackDecision:LOCAL_CHECK_ERROR] Error checking local file for ${track.id}: ${e.message}")
                    null
                }

                if (localFile != null && localFile.exists() && localFile.length() > 0) {
                    val fileUri = Uri.fromFile(localFile).toString()
                    Log.i(
                        TAG,
                        "[PlaybackDecision:PATH_TAKEN] -> [LOCAL_FILE] Playing downloaded local file (Offline/Online prioritized) for '${track.title}' (${track.id}): $fileUri (size=${localFile.length()} bytes)"
                    )
                    _state.update {
                        it.copy(
                            currentTrack = track,
                            resolvedStreamUrl = fileUri,
                            isBuffering = false,
                            errorMessage = null,
                            durationMs = if (track.durationSeconds > 0) track.durationSeconds * 1000L else it.durationMs
                        )
                    }
                    playStreamUrl(fileUri, track, startPositionMs)
                    recordHistory(track)
                    syncQueueToCloud()
                    return@launch
                } else {
                    Log.i(
                        TAG,
                        "[PlaybackDecision:LOCAL_FILE_ABSENT] No local downloaded file found for '${track.title}' (${track.id}). Proceeding to network stream extraction."
                    )
                }
            }

            // 2. Otherwise resolve stream over network
            val currentQuality = _state.value.videoQuality
            Log.i(
                TAG,
                "[PlaybackDecision:PATH_TAKEN] -> [NETWORK_STREAM] Streaming track '${track.title}' (${track.id}) over network (quality=$currentQuality)"
            )
            val result = streamResolver.resolveAudioStream(track.id, currentQuality)
            result.fold(
                onSuccess = { extracted ->
                    Log.i(
                        TAG,
                        "[PlaybackDecision:NETWORK_SUCCESS] Resolved stream for track.id='${track.id}' -> extracted.videoId='${extracted.videoId}', audioUrl='${extracted.streamUrl}', videoUrl='${extracted.videoStreamUrl}', bitrate=${extracted.bitrate} kbps"
                    )
                    val finalTrack = track.copy(
                        id = extracted.videoId,
                        title = if (track.title.isBlank() || track.title.startsWith("YouTube Audio")) extracted.title else track.title,
                        artist = if (track.artist.isBlank() || track.artist.startsWith("SWAR")) extracted.artist else track.artist,
                        durationSeconds = if (extracted.durationSeconds > 0) extracted.durationSeconds else track.durationSeconds,
                        thumbnailUrl = if (track.thumbnailUrl.isBlank()) extracted.thumbnailUrl else track.thumbnailUrl
                    )

                    _state.update {
                        it.copy(
                            currentTrack = finalTrack,
                            resolvedStreamUrl = extracted.streamUrl,
                            resolvedVideoStreamUrl = extracted.videoStreamUrl,
                            durationMs = if (extracted.durationSeconds > 0) extracted.durationSeconds * 1000L else it.durationMs
                        )
                    }

                    val targetUrl = if (_state.value.playbackMode == PlaybackMode.VIDEO && extracted.videoStreamUrl != null) {
                        extracted.videoStreamUrl
                    } else {
                        extracted.streamUrl
                    }

                    playStreamUrl(targetUrl, finalTrack, startPositionMs)
                    recordHistory(finalTrack)
                    syncQueueToCloud()
                },
                onFailure = { error ->
                    Log.e(TAG, "[PlaybackDecision:NETWORK_ERROR] Stream extraction failed for track '${track.id}': ${error.message}")
                    _state.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = false,
                            errorMessage = "Playback failed: ${error.localizedMessage ?: "Stream unavailable. Please check your internet connection."}"
                        )
                    }
                }
            )
        }
    }

    private fun playStreamUrl(streamUrl: String, track: Track, startPositionMs: Long) {
        try {
            Log.d(
                TAG,
                "[AudioTrace:Step-D] Setting MediaItem on ExoPlayer: mediaId='${track.id}', title='${track.title}', streamUrl='$streamUrl', startPositionMs=$startPositionMs"
            )

            // Stop and clear previous media item to prevent any stale audio mixing
            exoPlayer.stop()
            exoPlayer.clearMediaItems()

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setArtworkUri(if (track.thumbnailUrl.isNotBlank()) Uri.parse(track.thumbnailUrl) else null)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaId(track.id)
                .setMediaMetadata(mediaMetadata)
                .build()

            exoPlayer.setMediaItem(mediaItem, startPositionMs)
            exoPlayer.prepare()
            exoPlayer.play()
            ensureForegroundService()
            Log.d(TAG, "[AudioTrace:Step-D] ExoPlayer prepared & play() invoked for '${track.title}' (${track.id})")
        } catch (e: Exception) {
            Log.e(TAG, "[AudioTrace:Step-D-Error] Failed to start ExoPlayer stream for ${track.id}", e)
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "Player error: ${e.message}"
                )
            }
        }
    }

    fun playPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        exoPlayer.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        if (_state.value.currentTrack != null) {
            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.currentMediaItem == null) {
                _state.value.currentTrack?.let { resolveAndPlay(it, _state.value.currentPositionMs) }
            } else {
                exoPlayer.play()
                _state.update { it.copy(isPlaying = true) }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L).coerceAtMost(_state.value.durationMs.coerceAtLeast(0L))
        _state.update { it.copy(currentPositionMs = safePosition) }
        exoPlayer.seekTo(safePosition)
    }

    fun skipToNext() {
        val currentState = _state.value
        checkAndRecordQuickSkip(currentState.currentTrack)

        if (currentState.playbackOrigin == PlaybackOrigin.SEARCH) {
            if (currentState.repeatMode == RepeatMode.ONE) {
                currentState.currentTrack?.let { resolveAndPlay(it) }
                return
            }

            // If user previously went back into history, step forward in the real history
            if (searchHistoryIndex in 0 until searchHistoryStack.lastIndex) {
                searchHistoryIndex++
                val nextTrack = searchHistoryStack[searchHistoryIndex]
                Log.d(TAG, "[RadioEngine] Moving forward in history stack to: '${nextTrack.title}' (index $searchHistoryIndex/${searchHistoryStack.lastIndex})")
                _state.update {
                    it.copy(
                        currentIndex = searchHistoryIndex,
                        currentTrack = nextTrack,
                        isBuffering = true
                    )
                }
                resolveAndPlay(nextTrack)
                return
            }

            // At the head of history: dynamically generate next related song
            val currentTrack = currentState.currentTrack ?: searchHistoryStack.lastOrNull()
            if (currentTrack != null) {
                if (isGeneratingRadioTrack) return
                isGeneratingRadioTrack = true
                _state.update { it.copy(isBuffering = true) }
                nextRadioJob?.cancel()
                nextRadioJob = scope.launch {
                    try {
                        val playedHistoryIds = searchHistoryStack.map { it.id }.toSet()
                        val nextTrack = relatedTrackGenerator.getNextRelatedTrack(currentTrack, playedHistoryIds)
                        searchHistoryStack.add(nextTrack)
                        searchHistoryIndex = searchHistoryStack.lastIndex
                        Log.d(TAG, "[RadioEngine] Appended dynamically generated radio track to history: '${nextTrack.title}' (index $searchHistoryIndex)")
                        _state.update {
                            it.copy(
                                queue = searchHistoryStack.toList(),
                                currentIndex = searchHistoryIndex,
                                currentTrack = nextTrack
                            )
                        }
                        resolveAndPlay(nextTrack)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error generating next radio track: ${e.message}", e)
                        _state.update { it.copy(isBuffering = false) }
                    } finally {
                        isGeneratingRadioTrack = false
                    }
                }
            }
            return
        }

        // LIST-origin session: strictly use defined list queue order
        val queue = currentState.queue
        if (queue.isEmpty()) return

        val nextIndex = when (currentState.repeatMode) {
            RepeatMode.ONE -> currentState.currentIndex
            RepeatMode.ALL -> (currentState.currentIndex + 1) % queue.size
            RepeatMode.OFF -> {
                if (currentState.currentIndex + 1 < queue.size) {
                    currentState.currentIndex + 1
                } else -1
            }
        }

        if (nextIndex in queue.indices) {
            val nextTrack = queue[nextIndex]
            _state.update { it.copy(currentIndex = nextIndex, currentTrack = nextTrack) }
            resolveAndPlay(nextTrack)
        }
    }

    fun skipToPrevious() {
        val currentState = _state.value
        // If track played > 3 seconds, rewind to start
        if (currentState.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        checkAndRecordQuickSkip(currentState.currentTrack)

        if (currentState.playbackOrigin == PlaybackOrigin.SEARCH) {
            if (currentState.repeatMode == RepeatMode.ONE) {
                seekTo(0L)
                return
            }

            // SEARCH-origin session: Step backwards through real history stack
            if (searchHistoryIndex > 0 && searchHistoryIndex <= searchHistoryStack.lastIndex) {
                searchHistoryIndex--
                val prevTrack = searchHistoryStack[searchHistoryIndex]
                Log.d(TAG, "[RadioEngine] Stepping backwards in real history stack to: '${prevTrack.title}' (index $searchHistoryIndex/${searchHistoryStack.lastIndex})")
                _state.update {
                    it.copy(
                        currentIndex = searchHistoryIndex,
                        currentTrack = prevTrack,
                        isBuffering = true
                    )
                }
                resolveAndPlay(prevTrack)
            } else {
                seekTo(0L)
            }
            return
        }

        // LIST-origin session: strictly use defined list queue order
        val queue = currentState.queue
        if (queue.isEmpty()) return

        val prevIndex = when (currentState.repeatMode) {
            RepeatMode.ONE -> currentState.currentIndex
            RepeatMode.ALL -> if (currentState.currentIndex - 1 < 0) queue.size - 1 else currentState.currentIndex - 1
            RepeatMode.OFF -> if (currentState.currentIndex > 0) currentState.currentIndex - 1 else 0
        }

        if (prevIndex in queue.indices) {
            val prevTrack = queue[prevIndex]
            _state.update { it.copy(currentIndex = prevIndex, currentTrack = prevTrack) }
            resolveAndPlay(prevTrack)
        }
    }

    private fun handleTrackEnded() {
        val currentState = _state.value
        when (currentState.repeatMode) {
            RepeatMode.ONE -> {
                currentState.currentTrack?.let { resolveAndPlay(it) }
            }
            RepeatMode.ALL -> {
                skipToNext()
            }
            RepeatMode.OFF -> {
                if (currentState.playbackOrigin == PlaybackOrigin.SEARCH || currentState.hasNext) {
                    skipToNext()
                }
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        _state.update { it.copy(repeatMode = mode) }
    }

    fun toggleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    fun setVideoQuality(quality: VideoQuality) {
        if (_state.value.videoQuality == quality) return
        Log.i(TAG, "Setting video quality to $quality")
        _state.update { it.copy(videoQuality = quality) }

        val currentTrack = _state.value.currentTrack
        if (_state.value.playbackMode == PlaybackMode.VIDEO && currentTrack != null) {
            val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            resolveAndPlay(currentTrack, startPositionMs = currentPos, preferVideo = true)
        }
    }

    fun toggleShuffle() {
        _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun retry() {
        val currentTrack = _state.value.currentTrack ?: return
        _state.update { it.copy(errorMessage = null) }
        resolveAndPlay(currentTrack, _state.value.currentPositionMs)
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition
                    val dur = if (exoPlayer.duration > 0) exoPlayer.duration else _state.value.durationMs
                    _state.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun recordHistory(track: Track) {
        val user = authRepository?.getCurrentUser() ?: return
        if (user.uid.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                historyAndQueueRepository?.recordTrackPlayed(user.uid, track)
            }
        }
    }

    private fun syncQueueToCloud() {
        val user = authRepository?.getCurrentUser() ?: return
        if (user.uid.isBlank()) return

        val cur = _state.value
        val queueState = PlaybackQueueState(
            currentTrack = cur.currentTrack,
            queue = cur.queue,
            currentIndex = cur.currentIndex,
            playbackPositionMs = cur.currentPositionMs,
            isPlaying = cur.isPlaying,
            updatedAt = System.currentTimeMillis()
        )

        scope.launch(Dispatchers.IO) {
            historyAndQueueRepository?.savePlaybackQueue(user.uid, queueState)
        }
    }

    fun release() {
        stopProgressTracking()
        resolveJob?.cancel()
        exoPlayer.release()
    }
}
