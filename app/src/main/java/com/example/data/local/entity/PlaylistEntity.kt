package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CustomPlaylist
import com.example.data.model.ImportedYouTubePlaylist
import com.example.data.model.Track
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "local_playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val isImported: Boolean = false,
    val author: String = "",
    val originalPlaylistId: String = "",
    val tracksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toCustomPlaylist(): CustomPlaylist {
        return CustomPlaylist(
            id = id,
            name = title,
            description = description,
            tracks = parseTracks(tracksJson),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun toImportedPlaylist(): ImportedYouTubePlaylist {
        val parsedTracks = parseTracks(tracksJson)
        return ImportedYouTubePlaylist(
            id = id,
            youtubePlaylistId = originalPlaylistId.ifBlank { id },
            title = title,
            author = author,
            trackCount = parsedTracks.size,
            tracks = parsedTracks,
            importedAt = createdAt,
            lastSyncedAt = updatedAt
        )
    }

    companion object {
        fun serializeTracks(tracks: List<Track>): String {
            val jsonArray = JSONArray()
            for (track in tracks) {
                val obj = JSONObject()
                obj.put("id", track.id)
                obj.put("title", track.title)
                obj.put("artist", track.artist)
                obj.put("durationSeconds", track.durationSeconds)
                obj.put("thumbnailUrl", track.thumbnailUrl)
                obj.put("addedAt", track.addedAt)
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        fun parseTracks(json: String): List<Track> {
            if (json.isBlank()) return emptyList()
            val list = mutableListOf<Track>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Track(
                            id = obj.optString("id", ""),
                            title = obj.optString("title", "Unknown"),
                            artist = obj.optString("artist", "Unknown"),
                            durationSeconds = obj.optLong("durationSeconds", 0L),
                            thumbnailUrl = obj.optString("thumbnailUrl", ""),
                            addedAt = obj.optLong("addedAt", 0L)
                        )
                    )
                }
            } catch (ignored: Exception) {}
            return list
        }

        fun fromCustomPlaylist(playlist: CustomPlaylist): PlaylistEntity {
            return PlaylistEntity(
                id = playlist.id,
                title = playlist.name,
                description = playlist.description,
                isImported = false,
                tracksJson = serializeTracks(playlist.tracks),
                createdAt = playlist.createdAt,
                updatedAt = playlist.updatedAt
            )
        }

        fun fromImportedPlaylist(playlist: ImportedYouTubePlaylist): PlaylistEntity {
            return PlaylistEntity(
                id = playlist.id,
                title = playlist.title,
                description = "",
                isImported = true,
                author = playlist.author,
                originalPlaylistId = playlist.youtubePlaylistId,
                tracksJson = serializeTracks(playlist.tracks),
                createdAt = playlist.importedAt,
                updatedAt = playlist.lastSyncedAt
            )
        }
    }
}
