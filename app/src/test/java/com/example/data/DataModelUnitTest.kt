package com.example.data

import com.example.data.model.CustomPlaylist
import com.example.data.model.ImportedYouTubePlaylist
import com.example.data.model.LikedSong
import com.example.data.model.PlaybackQueueState
import com.example.data.model.RecentlyPlayedItem
import com.example.data.model.Track
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelUnitTest {

    @Test
    fun testTrackModelCreation() {
        val track = Track(
            id = "yt_12345",
            title = "Morning Raga",
            artist = "Pandit Hariprasad",
            durationSeconds = 480,
            thumbnailUrl = "https://example.com/thumb.jpg"
        )

        assertEquals("yt_12345", track.id)
        assertEquals("Morning Raga", track.title)
        assertEquals("Pandit Hariprasad", track.artist)
        assertEquals(480L, track.durationSeconds)
        assertTrue(track.addedAt > 0)
    }

    @Test
    fun testLikedSongModel() {
        val track = Track(id = "track_1", title = "Desi Beats", artist = "Artist A")
        val likedSong = LikedSong(id = track.id, track = track)

        assertEquals("track_1", likedSong.id)
        assertEquals("Desi Beats", likedSong.track.title)
        assertTrue(likedSong.likedAt > 0)
    }

    @Test
    fun testCustomPlaylistModel() {
        val tracks = listOf(
            Track(id = "t1", title = "Track 1", artist = "Artist 1"),
            Track(id = "t2", title = "Track 2", artist = "Artist 2")
        )
        val playlist = CustomPlaylist(
            id = "cp_001",
            name = "Workout Vibes",
            description = "High tempo fusion",
            tracks = tracks
        )

        assertEquals("cp_001", playlist.id)
        assertEquals("Workout Vibes", playlist.name)
        assertEquals(2, playlist.tracks.size)
        assertEquals("Track 1", playlist.tracks[0].title)
    }

    @Test
    fun testImportedYouTubePlaylistModel() {
        val tracks = listOf(Track(id = "yt_1", title = "Song A"))
        val ytPlaylist = ImportedYouTubePlaylist(
            id = "imp_01",
            youtubePlaylistId = "PL1234567890",
            title = "Top Hits 2026",
            author = "YouTube Music",
            trackCount = 1,
            tracks = tracks
        )

        assertEquals("imp_01", ytPlaylist.id)
        assertEquals("PL1234567890", ytPlaylist.youtubePlaylistId)
        assertEquals("Top Hits 2026", ytPlaylist.title)
        assertEquals(1, ytPlaylist.trackCount)
    }

    @Test
    fun testRecentlyPlayedAndQueueState() {
        val track = Track(id = "rp_1", title = "Evening Fusion")
        val recentItem = RecentlyPlayedItem(id = "rp_1", track = track)
        assertEquals("rp_1", recentItem.id)

        val queueState = PlaybackQueueState(
            currentTrack = track,
            queue = listOf(track),
            currentIndex = 0,
            playbackPositionMs = 45000L,
            isPlaying = true
        )

        assertNotNull(queueState.currentTrack)
        assertEquals(1, queueState.queue.size)
        assertEquals(45000L, queueState.playbackPositionMs)
        assertTrue(queueState.isPlaying)
    }

    @Test
    fun testUserProfileModel() {
        val profile = UserProfile(
            uid = "user_abc",
            email = "user@example.com",
            displayName = "Swar User",
            photoUrl = "https://example.com/avatar.png"
        )

        assertEquals("user_abc", profile.uid)
        assertEquals("user@example.com", profile.email)
        assertEquals("Swar User", profile.displayName)
    }

    @Test
    fun testDownloadedTrackEntityModel() {
        val entity = com.example.data.local.entity.DownloadedTrackEntity(
            trackId = "track_101",
            title = "Test Song",
            artist = "Test Artist",
            durationSeconds = 240,
            thumbnailUrl = "https://example.com/art.jpg",
            localFilePath = "/data/data/com.example/files/downloads/track_101.m4a",
            fileSizeBytes = 5242880L,
            downloadedAt = System.currentTimeMillis(),
            status = com.example.data.local.entity.DownloadStatus.COMPLETED,
            progress = 100,
            errorMessage = null
        )

        assertEquals("track_101", entity.trackId)
        assertEquals("Test Song", entity.title)
        assertEquals(com.example.data.local.entity.DownloadStatus.COMPLETED, entity.status)
        assertEquals(100, entity.progress)
        val track = entity.toTrack()
        assertEquals("track_101", track.id)
        assertEquals("Test Song", track.title)
    }
}
