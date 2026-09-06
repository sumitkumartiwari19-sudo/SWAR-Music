package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.QuickSkipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickSkipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickSkip(quickSkip: QuickSkipEntity)

    @Query("SELECT * FROM quick_skips ORDER BY skippedAt DESC LIMIT 200")
    suspend fun getRecentQuickSkips(): List<QuickSkipEntity>

    @Query("SELECT * FROM quick_skips ORDER BY skippedAt DESC LIMIT 200")
    fun observeRecentQuickSkips(): Flow<List<QuickSkipEntity>>

    @Query("SELECT COUNT(*) FROM quick_skips WHERE LOWER(artist) = LOWER(:artist)")
    suspend fun getQuickSkipCountForArtist(artist: String): Int

    @Query("DELETE FROM quick_skips")
    suspend fun clearAll()
}
