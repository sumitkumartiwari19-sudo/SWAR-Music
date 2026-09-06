package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LikedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedSongDao {

    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    fun getLikedSongs(): Flow<List<LikedSongEntity>>

    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    suspend fun getLikedSongsList(): List<LikedSongEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE trackId = :trackId)")
    fun isLikedFlow(trackId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE trackId = :trackId)")
    suspend fun isLiked(trackId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedSong(song: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE trackId = :trackId")
    suspend fun deleteLikedSong(trackId: String)

    @Query("DELETE FROM liked_songs")
    suspend fun clearAll()
}
