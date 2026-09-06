package com.example.data.repository

import android.content.Context
import com.example.data.local.SwarDatabase
import com.example.data.local.entity.HistoryTrackEntity
import com.example.data.model.PlaybackQueueState
import com.example.data.model.RecentlyPlayedItem
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface HistoryAndQueueRepository {
    // Recently Played
    fun getRecentlyPlayed(userId: String = "local_user", limit: Long = 50): Flow<List<RecentlyPlayedItem>>
    suspend fun recordTrackPlayed(userId: String = "local_user", track: Track): Result<Unit>
    suspend fun clearRecentlyPlayed(userId: String = "local_user"): Result<Unit>

    // Current Playback Queue (Cross-session resumption)
    fun getPlaybackQueue(userId: String = "local_user"): Flow<PlaybackQueueState?>
    suspend fun savePlaybackQueue(userId: String = "local_user", queueState: PlaybackQueueState): Result<Unit>
}

class HistoryAndQueueRepositoryImpl(
    private val database: SwarDatabase
) : HistoryAndQueueRepository {

    constructor(context: Context) : this(SwarDatabase.getInstance(context))

    private val dao = database.historyTrackDao()
    private val localQueue = MutableStateFlow<PlaybackQueueState?>(null)

    override fun getRecentlyPlayed(userId: String, limit: Long): Flow<List<RecentlyPlayedItem>> {
        return dao.getRecentlyPlayed(limit.toInt()).map { list ->
            list.map { it.toRecentlyPlayedItem() }
        }
    }

    override suspend fun recordTrackPlayed(userId: String, track: Track): Result<Unit> = withContext(Dispatchers.IO) {
        if (track.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Track ID must not be blank"))
        }
        try {
            dao.recordTrack(HistoryTrackEntity.fromTrack(track, System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearRecentlyPlayed(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPlaybackQueue(userId: String): Flow<PlaybackQueueState?> {
        return localQueue.asStateFlow()
    }

    override suspend fun savePlaybackQueue(
        userId: String,
        queueState: PlaybackQueueState
    ): Result<Unit> = withContext(Dispatchers.IO) {
        localQueue.value = queueState
        Result.success(Unit)
    }
}
