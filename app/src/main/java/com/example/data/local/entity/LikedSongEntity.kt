package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.LikedSong
import com.example.data.model.Track

@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey
    val trackId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val likedAt: Long = System.currentTimeMillis()
) {
    fun toLikedSong(): LikedSong {
        return LikedSong(
            id = trackId,
            track = Track(
                id = trackId,
                title = title,
                artist = artist,
                durationSeconds = durationSeconds,
                thumbnailUrl = thumbnailUrl,
                addedAt = likedAt
            ),
            likedAt = likedAt
        )
    }

    fun toTrack(): Track {
        return Track(
            id = trackId,
            title = title,
            artist = artist,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnailUrl,
            addedAt = likedAt
        )
    }

    companion object {
        fun fromTrack(track: Track, likedAt: Long = System.currentTimeMillis()): LikedSongEntity {
            return LikedSongEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                durationSeconds = track.durationSeconds,
                thumbnailUrl = track.thumbnailUrl,
                likedAt = likedAt
            )
        }
    }
}
