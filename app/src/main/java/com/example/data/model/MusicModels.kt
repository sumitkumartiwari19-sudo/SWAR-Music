package com.example.data.model

import androidx.annotation.Keep

@Keep
data class Track(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val durationSeconds: Long = 0L,
    val thumbnailUrl: String = "",
    val channelId: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Keep
data class LikedSong(
    val id: String = "", // Same as track.id
    val track: Track = Track(),
    val likedAt: Long = System.currentTimeMillis()
)

@Keep
data class CustomPlaylist(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class ImportedYouTubePlaylist(
    val id: String = "",
    val youtubePlaylistId: String = "",
    val title: String = "",
    val author: String = "",
    val trackCount: Int = 0,
    val tracks: List<Track> = emptyList(),
    val importedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Keep
data class RecentlyPlayedItem(
    val id: String = "", // Unique ID or track.id
    val track: Track = Track(),
    val playedAt: Long = System.currentTimeMillis()
)

@Keep
data class PlaybackQueueState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val playbackPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
