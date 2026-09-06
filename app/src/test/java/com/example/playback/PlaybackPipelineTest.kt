package com.example.playback

import com.example.data.model.Track
import com.example.data.repository.PlayerRepository
import com.example.playback.extractor.AudioStreamResolver
import com.example.ui.playback.PlaybackViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackPipelineTest {

    private val track1 = Track(
        id = "track_001",
        title = "Track One",
        artist = "Artist One",
        durationSeconds = 200,
        thumbnailUrl = "https://example.com/art1.jpg"
    )

    private val track2 = Track(
        id = "track_002",
        title = "Track Two",
        artist = "Artist Two",
        durationSeconds = 300,
        thumbnailUrl = "https://example.com/art2.jpg"
    )

    @Test
    fun playbackState_progressCalculation() {
        val state = PlaybackState(
            currentTrack = track1,
            currentPositionMs = 50000L,
            durationMs = 200000L
        )

        assertEquals(0.25f, state.progress, 0.001f)
    }

    @Test
    fun playbackState_navigationFlags() {
        val queue = listOf(track1, track2)
        val stateFirst = PlaybackState(
            currentTrack = track1,
            queue = queue,
            currentIndex = 0,
            repeatMode = RepeatMode.OFF
        )
        assertTrue(stateFirst.hasNext)
        assertFalse(stateFirst.hasPrevious)

        val stateSecond = PlaybackState(
            currentTrack = track2,
            queue = queue,
            currentIndex = 1,
            repeatMode = RepeatMode.OFF
        )
        assertFalse(stateSecond.hasNext)
        assertTrue(stateSecond.hasPrevious)

        val stateRepeatAll = stateSecond.copy(repeatMode = RepeatMode.ALL)
        assertTrue(stateRepeatAll.hasNext)
        assertTrue(stateRepeatAll.hasPrevious)

        // SEARCH origin always has next (radio continuation) and previous (history walkback)
        val stateSearchOrigin = PlaybackState(
            currentTrack = track1,
            queue = listOf(track1),
            currentIndex = 0,
            repeatMode = RepeatMode.OFF,
            playbackOrigin = PlaybackOrigin.SEARCH
        )
        assertTrue(stateSearchOrigin.hasNext)
        assertTrue(stateSearchOrigin.hasPrevious)
    }

    @Test
    fun musicKeywordExtractor_extractsKeywordsAndNormalizes() {
        val keywords = com.example.playback.extractor.MusicKeywordExtractor.extractKeywords(
            "Kesariya (Official Audio Video) - Brahmastra",
            "Arijit Singh - Topic"
        )
        assertTrue(keywords.contains("kesariya"))
        assertTrue(keywords.contains("brahmastra"))
        assertTrue(keywords.contains("arijit"))
        assertTrue(keywords.contains("singh"))
        assertFalse(keywords.contains("official"))
        assertFalse(keywords.contains("audio"))
        assertFalse(keywords.contains("video"))
        assertFalse(keywords.contains("topic"))

        val normArtist = com.example.playback.extractor.MusicKeywordExtractor.normalizeArtist("Arijit Singh - Topic")
        assertEquals("arijit singh", normArtist)
    }

    @Test
    fun audioStreamResolver_testTracksResolveReliably() = runTest {
        val resolver = AudioStreamResolver()
        val result = resolver.resolveAudioStream("test_raga_01")

        assertTrue(result.isSuccess)
        val extracted = result.getOrNull()
        assertNotNull(extracted)
        assertEquals("test_raga_01", extracted?.videoId)
        assertTrue(extracted?.streamUrl?.startsWith("http") == true)
    }

    @Test
    fun fakePlayerRepository_stateTransitions() {
        val fakeRepo = FakePlayerRepository()
        assertEquals(false, fakeRepo.state.value.isPlaying)

        fakeRepo.playTrack(track1, listOf(track1, track2))
        assertEquals(track1, fakeRepo.state.value.currentTrack)
        assertEquals(0, fakeRepo.state.value.currentIndex)
        assertEquals(true, fakeRepo.state.value.isPlaying)

        fakeRepo.skipToNext()
        assertEquals(track2, fakeRepo.state.value.currentTrack)
        assertEquals(1, fakeRepo.state.value.currentIndex)

        fakeRepo.pause()
        assertEquals(false, fakeRepo.state.value.isPlaying)
    }

    private class FakePlayerRepository : PlayerRepository {
        private val _state = MutableStateFlow(PlaybackState())
        override val state: StateFlow<PlaybackState> = _state.asStateFlow()
        override val exoPlayer: androidx.media3.exoplayer.ExoPlayer
            get() = throw UnsupportedOperationException("Not needed in unit test")

        override fun playTrack(track: Track, queue: List<Track>, origin: PlaybackOrigin) {
            val q = if (queue.isNotEmpty()) queue else listOf(track)
            val idx = q.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            _state.value = _state.value.copy(
                currentTrack = track,
                queue = q,
                currentIndex = idx,
                playbackOrigin = origin,
                isPlaying = true,
                durationMs = track.durationSeconds * 1000L
            )
        }

        override fun playYouTubeVideoId(
            videoId: String,
            title: String?,
            artist: String?,
            thumbnailUrl: String?,
            origin: PlaybackOrigin
        ) {
            val t = Track(videoId, title ?: "Test", artist ?: "Test", thumbnailUrl = thumbnailUrl ?: "")
            playTrack(t, origin = origin)
        }

        override fun playPause() {
            _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
        }

        override fun pause() {
            _state.value = _state.value.copy(isPlaying = false)
        }

        override fun resume() {
            _state.value = _state.value.copy(isPlaying = true)
        }

        override fun seekTo(positionMs: Long) {
            _state.value = _state.value.copy(currentPositionMs = positionMs)
        }

        override fun skipToNext() {
            val q = _state.value.queue
            val next = _state.value.currentIndex + 1
            if (next in q.indices) {
                _state.value = _state.value.copy(currentIndex = next, currentTrack = q[next])
            }
        }

        override fun skipToPrevious() {
            val q = _state.value.queue
            val prev = _state.value.currentIndex - 1
            if (prev in q.indices) {
                _state.value = _state.value.copy(currentIndex = prev, currentTrack = q[prev])
            }
        }

        override fun setRepeatMode(mode: RepeatMode) {
            _state.value = _state.value.copy(repeatMode = mode)
        }

        override fun toggleRepeatMode() {
            val next = when (_state.value.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            setRepeatMode(next)
        }

        override fun toggleShuffle() {
            _state.value = _state.value.copy(shuffleEnabled = !_state.value.shuffleEnabled)
        }

        override fun setPlaybackMode(mode: PlaybackMode) {
            _state.value = _state.value.copy(playbackMode = mode)
        }

        override fun setVideoQuality(quality: com.example.playback.VideoQuality) {
            _state.value = _state.value.copy(videoQuality = quality)
        }

        override fun retry() {
            _state.value = _state.value.copy(errorMessage = null)
        }
    }
}
