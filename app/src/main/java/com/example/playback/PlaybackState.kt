package com.example.playback

import com.example.data.model.Track

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class PlaybackMode {
    AUDIO,
    VIDEO
}

enum class PlaybackOrigin {
    SEARCH,
    LIST
}

enum class VideoQuality(val label: String, val height: Int) {
    AUTO("Auto", 0),
    Q1080P("1080p", 1080),
    Q720P("720p", 720),
    Q480P("480p", 480);

    companion object {
        fun fromLabel(label: String): VideoQuality {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: AUTO
        }
    }
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val errorMessage: String? = null,
    val resolvedStreamUrl: String? = null,
    val resolvedVideoStreamUrl: String? = null,
    val playbackMode: PlaybackMode = PlaybackMode.AUDIO,
    val videoQuality: VideoQuality = VideoQuality.AUTO,
    val audioBitrateKbps: Int? = null,
    val playbackOrigin: PlaybackOrigin = PlaybackOrigin.LIST
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val hasNext: Boolean
        get() = when {
            playbackOrigin == PlaybackOrigin.SEARCH -> true
            repeatMode == RepeatMode.ONE -> true
            repeatMode == RepeatMode.ALL -> queue.isNotEmpty()
            repeatMode == RepeatMode.OFF -> currentIndex < queue.size - 1
            else -> true
        }

    val hasPrevious: Boolean
        get() = when {
            playbackOrigin == PlaybackOrigin.SEARCH -> true
            repeatMode == RepeatMode.ONE -> true
            repeatMode == RepeatMode.ALL -> queue.isNotEmpty()
            repeatMode == RepeatMode.OFF -> currentIndex > 0
            else -> true
        }
}

