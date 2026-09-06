package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.HistoryTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryTrackDao {

    @Query("SELECT * FROM history_tracks ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<HistoryTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordTrack(track: HistoryTrackEntity)

    @Query("DELETE FROM history_tracks WHERE trackId = :trackId")
    suspend fun deleteTrack(trackId: String)

    @Query("DELETE FROM history_tracks")
    suspend fun clearAll()
}
