package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.RecentlyPlayedItem
import com.example.data.model.Track

@Entity(tableName = "history_tracks")
data class HistoryTrackEntity(
    @PrimaryKey
    val trackId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toRecentlyPlayedItem(): RecentlyPlayedItem {
        return RecentlyPlayedItem(
            id = trackId,
            track = Track(
                id = trackId,
                title = title,
                artist = artist,
                durationSeconds = durationSeconds,
                thumbnailUrl = thumbnailUrl,
                addedAt = playedAt
            ),
            playedAt = playedAt
        )
    }

    fun toTrack(): Track {
        return Track(
            id = trackId,
            title = title,
            artist = artist,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnailUrl,
            addedAt = playedAt
        )
    }

    companion object {
        fun fromTrack(track: Track, playedAt: Long = System.currentTimeMillis()): HistoryTrackEntity {
            return HistoryTrackEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                durationSeconds = track.durationSeconds,
                thumbnailUrl = track.thumbnailUrl,
                playedAt = playedAt
            )
        }
    }
}
