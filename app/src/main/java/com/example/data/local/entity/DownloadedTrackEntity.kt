package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Track

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

@Entity(
    tableName = "downloaded_tracks",
    indices = [Index(value = ["trackId"], unique = true)]
)
data class DownloadedTrackEntity(
    @PrimaryKey
    val trackId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val localFilePath: String,
    val fileSizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val status: DownloadStatus = DownloadStatus.COMPLETED,
    val progress: Int = 100,
    val errorMessage: String? = null
) {
    fun toTrack(): Track {
        return Track(
            id = trackId,
            title = title,
            artist = artist,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnailUrl,
            addedAt = downloadedAt
        )
    }

    companion object {
        fun fromTrack(
            track: Track,
            localFilePath: String,
            fileSizeBytes: Long = 0L,
            status: DownloadStatus = DownloadStatus.PENDING,
            progress: Int = 0
        ): DownloadedTrackEntity {
            return DownloadedTrackEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                durationSeconds = track.durationSeconds,
                thumbnailUrl = track.thumbnailUrl,
                localFilePath = localFilePath,
                fileSizeBytes = fileSizeBytes,
                downloadedAt = System.currentTimeMillis(),
                status = status,
                progress = progress
            )
        }
    }
}
