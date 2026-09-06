package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM local_playlists WHERE isImported = 0 ORDER BY updatedAt DESC")
    fun getCustomPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM local_playlists WHERE isImported = 1 ORDER BY createdAt DESC")
    fun getImportedPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM local_playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(playlist: PlaylistEntity)

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Query("DELETE FROM local_playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: String)

    @Query("DELETE FROM local_playlists WHERE isImported = :isImported")
    suspend fun deleteAllByType(isImported: Boolean)
}
