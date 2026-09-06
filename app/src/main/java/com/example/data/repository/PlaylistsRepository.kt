package com.example.data.repository

import android.content.Context
import com.example.data.local.SwarDatabase
import com.example.data.local.entity.PlaylistEntity
import com.example.data.model.CuratedTracks
import com.example.data.model.CustomPlaylist
import com.example.data.model.ImportedYouTubePlaylist
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

interface PlaylistsRepository {
    // Custom playlists
    fun getCustomPlaylists(userId: String = "local_user"): Flow<List<CustomPlaylist>>
    suspend fun getCustomPlaylist(userId: String = "local_user", playlistId: String): Result<CustomPlaylist?>
    suspend fun createCustomPlaylist(userId: String = "local_user", name: String, description: String = ""): Result<CustomPlaylist>
    suspend fun addTrackToPlaylist(userId: String = "local_user", playlistId: String, track: Track): Result<Unit>
    suspend fun updatePlaylistTracks(userId: String = "local_user", playlistId: String, tracks: List<Track>): Result<Unit>
    suspend fun removeTrackFromPlaylist(userId: String = "local_user", playlistId: String, trackId: String): Result<Unit>
    suspend fun deleteCustomPlaylist(userId: String = "local_user", playlistId: String): Result<Unit>

    // Imported YouTube playlists
    fun getImportedPlaylists(userId: String = "local_user"): Flow<List<ImportedYouTubePlaylist>>
    suspend fun getImportedPlaylist(userId: String = "local_user", playlistId: String): Result<ImportedYouTubePlaylist?>
    suspend fun saveImportedPlaylist(userId: String = "local_user", playlist: ImportedYouTubePlaylist): Result<Unit>
    suspend fun deleteImportedPlaylist(userId: String = "local_user", playlistId: String): Result<Unit>
}

class PlaylistsRepositoryImpl(
    private val database: SwarDatabase
) : PlaylistsRepository {

    constructor(context: Context) : this(SwarDatabase.getInstance(context))

    private val dao = database.playlistDao()

    override fun getCustomPlaylists(userId: String): Flow<List<CustomPlaylist>> {
        return dao.getCustomPlaylists().map { entities ->
            if (entities.isEmpty()) {
                // Return default custom playlists if empty
                listOf(
                    CustomPlaylist(
                        id = "default_my_fav_songs",
                        name = "my favourite song",
                        description = "Bollywood & Sufi favorite gems",
                        tracks = CuratedTracks.allCurated,
                        createdAt = System.currentTimeMillis() - 86400000L,
                        updatedAt = System.currentTimeMillis()
                    ),
                    CustomPlaylist(
                        id = "default_midnight_chill",
                        name = "Midnight Lo-Fi & Ragas",
                        description = "Relaxing Indian instrumental & chill beats",
                        tracks = CuratedTracks.quickPicks + CuratedTracks.trendingNow,
                        createdAt = System.currentTimeMillis() - 172800000L,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                entities.map { it.toCustomPlaylist() }
            }
        }
    }

    override suspend fun getCustomPlaylist(userId: String, playlistId: String): Result<CustomPlaylist?> = withContext(Dispatchers.IO) {
        val entity = dao.getPlaylistById(playlistId)
        if (entity != null) {
            return@withContext Result.success(entity.toCustomPlaylist())
        }
        // Fallback check default playlists
        if (playlistId == "default_my_fav_songs") {
            return@withContext Result.success(
                CustomPlaylist(
                    id = "default_my_fav_songs",
                    name = "my favourite song",
                    description = "Bollywood & Sufi favorite gems",
                    tracks = CuratedTracks.allCurated,
                    createdAt = System.currentTimeMillis() - 86400000L,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        if (playlistId == "default_midnight_chill") {
            return@withContext Result.success(
                CustomPlaylist(
                    id = "default_midnight_chill",
                    name = "Midnight Lo-Fi & Ragas",
                    description = "Relaxing Indian instrumental & chill beats",
                    tracks = CuratedTracks.quickPicks + CuratedTracks.trendingNow,
                    createdAt = System.currentTimeMillis() - 172800000L,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        Result.success(null)
    }

    override suspend fun createCustomPlaylist(
        userId: String,
        name: String,
        description: String
    ): Result<CustomPlaylist> = withContext(Dispatchers.IO) {
        if (name.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Playlist Name must not be blank"))
        }
        val playlistId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val playlist = CustomPlaylist(
            id = playlistId,
            name = name.trim(),
            description = description.trim(),
            tracks = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        try {
            dao.insertOrUpdate(PlaylistEntity.fromCustomPlaylist(playlist))
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTrackToPlaylist(userId: String, playlistId: String, track: Track): Result<Unit> = withContext(Dispatchers.IO) {
        if (playlistId.isBlank() || track.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid parameters for addTrackToPlaylist"))
        }
        try {
            val entity = dao.getPlaylistById(playlistId)
            val currentTracks = if (entity != null) PlaylistEntity.parseTracks(entity.tracksJson) else emptyList()
            val updatedTracks = currentTracks.filterNot { it.id == track.id } + track
            val updatedEntity = (entity ?: PlaylistEntity(
                id = playlistId,
                title = "Playlist",
                createdAt = System.currentTimeMillis()
            )).copy(
                tracksJson = PlaylistEntity.serializeTracks(updatedTracks),
                updatedAt = System.currentTimeMillis()
            )
            dao.insertOrUpdate(updatedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePlaylistTracks(
        userId: String,
        playlistId: String,
        tracks: List<Track>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getPlaylistById(playlistId)
            if (entity != null) {
                dao.insertOrUpdate(
                    entity.copy(
                        tracksJson = PlaylistEntity.serializeTracks(tracks),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeTrackFromPlaylist(
        userId: String,
        playlistId: String,
        trackId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getPlaylistById(playlistId)
            if (entity != null) {
                val currentTracks = PlaylistEntity.parseTracks(entity.tracksJson)
                val updated = currentTracks.filterNot { it.id == trackId }
                dao.insertOrUpdate(
                    entity.copy(
                        tracksJson = PlaylistEntity.serializeTracks(updated),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCustomPlaylist(userId: String, playlistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteById(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getImportedPlaylists(userId: String): Flow<List<ImportedYouTubePlaylist>> {
        return dao.getImportedPlaylists().map { entities ->
            if (entities.isEmpty()) {
                listOf(
                    ImportedYouTubePlaylist(
                        id = "PL_bollywood_top_hits",
                        youtubePlaylistId = "PL_bollywood_top_hits",
                        title = "Bollywood Top Hits 2026",
                        author = "T-Series & Saregama",
                        trackCount = CuratedTracks.trendingNow.size + CuratedTracks.newReleasesAndRegional.size,
                        tracks = CuratedTracks.trendingNow + CuratedTracks.newReleasesAndRegional,
                        importedAt = System.currentTimeMillis(),
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
            } else {
                entities.map { it.toImportedPlaylist() }
            }
        }
    }

    override suspend fun getImportedPlaylist(
        userId: String,
        playlistId: String
    ): Result<ImportedYouTubePlaylist?> = withContext(Dispatchers.IO) {
        val entity = dao.getPlaylistById(playlistId)
        if (entity != null) {
            return@withContext Result.success(entity.toImportedPlaylist())
        }
        if (playlistId == "PL_bollywood_top_hits") {
            return@withContext Result.success(
                ImportedYouTubePlaylist(
                    id = "PL_bollywood_top_hits",
                    youtubePlaylistId = "PL_bollywood_top_hits",
                    title = "Bollywood Top Hits 2026",
                    author = "T-Series & Saregama",
                    trackCount = CuratedTracks.trendingNow.size + CuratedTracks.newReleasesAndRegional.size,
                    tracks = CuratedTracks.trendingNow + CuratedTracks.newReleasesAndRegional,
                    importedAt = System.currentTimeMillis(),
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
        }
        Result.success(null)
    }

    override suspend fun saveImportedPlaylist(
        userId: String,
        playlist: ImportedYouTubePlaylist
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.insertOrUpdate(PlaylistEntity.fromImportedPlaylist(playlist))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteImportedPlaylist(userId: String, playlistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteById(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
