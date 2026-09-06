package com.example.playback

import com.example.data.model.Track
import com.example.playback.extractor.MusicContentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MusicContentFilterTest {

    @Test
    fun testValidSongsPassFilter() {
        val validSong1 = Track(
            id = "song1",
            title = "Kesariya - Audio | Brahmāstra | Ranbir, Alia | Pritam | Arijit Singh",
            artist = "Sony Music India",
            durationSeconds = 268
        )
        val validSong2 = Track(
            id = "song2",
            title = "Chaleya (From 'Jawan')",
            artist = "Arijit Singh - Topic",
            durationSeconds = 200
        )
        val validSong3 = Track(
            id = "song3",
            title = "Raga Bhairavi Classical Sitar Solo",
            artist = "Pandit Ravi Shankar Legacy",
            durationSeconds = 420
        )
        val validSong4 = Track(
            id = "song4",
            title = "Tauba Tauba | Bad Newz | Vicky Kaushal | Karan Aujla",
            artist = "Saregama Music",
            durationSeconds = 207
        )

        assertTrue("Valid song 1 should pass", MusicContentFilter.isLikelySong(validSong1))
        assertTrue("Valid song 2 should pass", MusicContentFilter.isLikelySong(validSong2))
        assertTrue("Valid song 3 should pass", MusicContentFilter.isLikelySong(validSong3))
        assertTrue("Valid song 4 should pass", MusicContentFilter.isLikelySong(validSong4))
    }

    @Test
    fun testMoviesAndTrailersAreBlocked() {
        val fullMovie = Track(
            id = "movie1",
            title = "Pathaan Full Movie HD 1080p | Shah Rukh Khan | Deepika Padukone",
            artist = "YRF Cinema Vault",
            durationSeconds = 8500 // 2+ hours
        )
        val trailer = Track(
            id = "trailer1",
            title = "Jawan Official Trailer | Shah Rukh Khan | Atlee | Nayanthara",
            artist = "Red Chillies Entertainment",
            durationSeconds = 180
        )
        val teaser = Track(
            id = "teaser1",
            title = "Kalki 2898 AD Official Teaser Glimpse | Prabhas | Amitabh Bachchan",
            artist = "Vyjayanthi Movies",
            durationSeconds = 95
        )
        val shortFilm = Track(
            id = "short1",
            title = "The Chaiwala - Award Winning Short Film",
            artist = "Indie Cinema",
            durationSeconds = 900
        )

        assertFalse("Full movie should be blocked", MusicContentFilter.isLikelySong(fullMovie))
        assertFalse("Trailer should be blocked", MusicContentFilter.isLikelySong(trailer))
        assertFalse("Teaser should be blocked", MusicContentFilter.isLikelySong(teaser))
        assertFalse("Short film should be blocked", MusicContentFilter.isLikelySong(shortFilm))
    }

    @Test
    fun testPodcastsVlogsAndInterviewsAreBlocked() {
        val podcast = Track(
            id = "pod1",
            title = "Ranveer Show Podcast Ep 245 - Arijit Singh Untold Journey",
            artist = "BeerBiceps",
            durationSeconds = 4200
        )
        val interview = Track(
            id = "int1",
            title = "A.R. Rahman Exclusive Interview on 30 Years of Music",
            artist = "Film Companion",
            durationSeconds = 1500
        )
        val vlog = Track(
            id = "vlog1",
            title = "Mumbai to Goa Road Trip Vlog! Behind the Scenes",
            artist = "Flying Beast",
            durationSeconds = 800
        )
        val reaction = Track(
            id = "react1",
            title = "Foreigners Reaction to Kesariya Song for the First Time!",
            artist = "React Squad",
            durationSeconds = 600
        )

        assertFalse("Podcast should be blocked", MusicContentFilter.isLikelySong(podcast))
        assertFalse("Interview should be blocked", MusicContentFilter.isLikelySong(interview))
        assertFalse("Vlog should be blocked", MusicContentFilter.isLikelySong(vlog))
        assertFalse("Reaction should be blocked", MusicContentFilter.isLikelySong(reaction))
    }

    @Test
    fun testDurationWindowEnforcement() {
        val snippet = Track(
            id = "snip1",
            title = "Best Song Ringtone 2026",
            artist = "Ringtone Hub",
            durationSeconds = 25 // < 45 seconds
        )
        val overLength = Track(
            id = "long1",
            title = "Entire Film Audio Commentary Track",
            artist = "Movie Buffs",
            durationSeconds = 3600 // 1 hour
        )

        assertFalse("Short snippet under 45s should be rejected", MusicContentFilter.isLikelySong(snippet))
        assertFalse("Non-jukebox over 13m should be rejected", MusicContentFilter.isLikelySong(overLength))
    }

    @Test
    fun testRankingBoostsTopicAndMusicLabels() {
        val topicTrack = Track(
            id = "t1",
            title = "Tum Hi Ho",
            artist = "Arijit Singh - Topic",
            durationSeconds = 262
        )
        val labelTrack = Track(
            id = "t2",
            title = "Tum Hi Ho Official Video Song",
            artist = "T-Series",
            durationSeconds = 262
        )
        val userUploadTrack = Track(
            id = "t3",
            title = "Tum Hi Ho Guitar Cover by Rahul",
            artist = "Rahul Music Studio",
            durationSeconds = 262
        )

        val unranked = listOf(userUploadTrack, topicTrack, labelTrack)
        val ranked = MusicContentFilter.filterAndRankSongs(unranked, "Tum Hi Ho")

        assertEquals("Topic track or official label should be first", topicTrack.id, ranked[0].id)
        assertEquals("Label track should be second", labelTrack.id, ranked[1].id)
        assertEquals("User upload should be third", userUploadTrack.id, ranked[2].id)
    }
}
