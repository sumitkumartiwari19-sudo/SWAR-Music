package com.example.data.repository

import android.content.Context
import com.example.data.local.SwarDatabase
import com.example.data.local.entity.LikedSongEntity
import com.example.data.model.LikedSong
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface LikedSongsRepository {
    fun getLikedSongs(userId: String = "local_user"): Flow<List<LikedSong>>
    fun isSongLiked(userId: String = "local_user", trackId: String): Flow<Boolean>
    suspend fun addLikedSong(userId: String = "local_user", track: Track): Result<Unit>
    suspend fun removeLikedSong(userId: String = "local_user", trackId: String): Result<Unit>
    suspend fun toggleLikedSong(userId: String = "local_user", track: Track): Result<Boolean>
}

class LikedSongsRepositoryImpl(
    private val database: SwarDatabase
) : LikedSongsRepository {

    constructor(context: Context) : this(SwarDatabase.getInstance(context))

    private val dao = database.likedSongDao()

    override fun getLikedSongs(userId: String): Flow<List<LikedSong>> {
        return dao.getLikedSongs().map { list ->
            list.map { it.toLikedSong() }
        }
    }

    override fun isSongLiked(userId: String, trackId: String): Flow<Boolean> {
        if (trackId.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(false)
        }
        return dao.isLikedFlow(trackId)
    }

    override suspend fun addLikedSong(userId: String, track: Track): Result<Unit> = withContext(Dispatchers.IO) {
        if (track.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Track ID must not be empty"))
        }
        try {
            dao.insertLikedSong(LikedSongEntity.fromTrack(track))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeLikedSong(userId: String, trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (trackId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Track ID must not be empty"))
        }
        try {
            dao.deleteLikedSong(trackId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleLikedSong(userId: String, track: Track): Result<Boolean> = withContext(Dispatchers.IO) {
        if (track.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Track ID must not be empty"))
        }
        try {
            val isLiked = dao.isLiked(track.id)
            if (isLiked) {
                dao.deleteLikedSong(track.id)
                Result.success(false)
            } else {
                dao.insertLikedSong(LikedSongEntity.fromTrack(track))
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
