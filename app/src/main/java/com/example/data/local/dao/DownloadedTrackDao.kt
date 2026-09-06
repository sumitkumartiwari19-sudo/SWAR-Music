package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DownloadStatus
import com.example.data.local.entity.DownloadedTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTrackDao {

    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAt DESC")
    fun getAllDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE status = 'COMPLETED' ORDER BY downloadedAt DESC")
    fun getCompletedDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId LIMIT 1")
    fun getDownloadedTrackFlow(trackId: String): Flow<DownloadedTrackEntity?>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId LIMIT 1")
    suspend fun getDownloadedTrack(trackId: String): DownloadedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(track: DownloadedTrackEntity)

    @Query("UPDATE downloaded_tracks SET progress = :progress, fileSizeBytes = :fileSizeBytes, status = :status WHERE trackId = :trackId")
    suspend fun updateProgress(trackId: String, progress: Int, fileSizeBytes: Long, status: DownloadStatus)

    @Query("UPDATE downloaded_tracks SET status = 'COMPLETED', progress = 100, fileSizeBytes = :fileSizeBytes, localFilePath = :localFilePath, downloadedAt = :downloadedAt, errorMessage = NULL WHERE trackId = :trackId")
    suspend fun markCompleted(trackId: String, fileSizeBytes: Long, localFilePath: String, downloadedAt: Long)

    @Query("UPDATE downloaded_tracks SET status = 'FAILED', progress = 0, errorMessage = :errorMessage WHERE trackId = :trackId")
    suspend fun markFailed(trackId: String, errorMessage: String)

    @Update
    suspend fun update(track: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String)

    @Query("DELETE FROM downloaded_tracks")
    suspend fun deleteAll()

    @Query("SELECT SUM(fileSizeBytes) FROM downloaded_tracks WHERE status = 'COMPLETED'")
    fun getTotalStorageSizeBytes(): Flow<Long?>
}
