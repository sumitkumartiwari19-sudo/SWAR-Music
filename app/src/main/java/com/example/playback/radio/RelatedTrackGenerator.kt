package com.example.playback.radio

import android.util.Log
import com.example.data.local.dao.QuickSkipDao
import com.example.data.model.CuratedTracks
import com.example.data.model.Track
import com.example.playback.extractor.MusicContentFilter
import com.example.playback.extractor.MusicKeywordExtractor
import com.example.playback.extractor.NewPipeDownloaderImpl
import com.example.playback.extractor.YouTubeSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Dynamic "Next Related Song" Radio Generator for SEARCH-origin playback sessions.
 * Combines NewPipe / YouTube Mix related recommendations (Source A) and supplementary keyword search (Source B),
 * then applies soft cumulative penalties from implicit quick-skip feedback before picking the next track.
 */
class RelatedTrackGenerator(
    private val quickSkipDao: QuickSkipDao? = null,
    private val searchService: YouTubeSearchService = YouTubeSearchService(),
    private val downloader: NewPipeDownloaderImpl = NewPipeDownloaderImpl.getInstance()
) {
    companion object {
        private const val TAG = "RelatedTrackGenerator"
    }

    init {
        NewPipeDownloaderImpl.initGlobal()
    }

    /**
     * Finds the next best related radio track for [currentTrack], avoiding previously played [playedHistoryTrackIds].
     */
    suspend fun getNextRelatedTrack(
        currentTrack: Track,
        playedHistoryTrackIds: Set<String> = emptySet()
    ): Track = withContext(Dispatchers.IO) {
        Log.i(TAG, "[RadioEngine:START] Generating next related track for '${currentTrack.title}' by '${currentTrack.artist}' (${currentTrack.id})")

        // 1. Fetch Candidates from Source A (NewPipe Related / Mix) and Source B (Keyword search) in parallel
        val sourceADeferred = async { fetchSourceARelatedTracks(currentTrack) }
        val sourceBDeferred = async { fetchSourceBSupplementaryTracks(currentTrack) }

        val sourceATracks = sourceADeferred.await()
        val sourceBTracks = sourceBDeferred.await()

        Log.d(TAG, "[RadioEngine:FETCH] Source A yielded ${sourceATracks.size} tracks, Source B yielded ${sourceBTracks.size} tracks")

        // 2. Fetch recent quick-skips for implicit negative feedback
        val recentQuickSkips = try {
            quickSkipDao?.getRecentQuickSkips() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch quick skips: ${e.message}")
            emptyList()
        }

        val skippedArtistCounts = mutableMapOf<String, Int>()
        val skippedKeywordCounts = mutableMapOf<String, Int>()

        for (skip in recentQuickSkips) {
            val normArtist = MusicKeywordExtractor.normalizeArtist(skip.artist)
            if (normArtist.isNotBlank()) {
                skippedArtistCounts[normArtist] = (skippedArtistCounts[normArtist] ?: 0) + 1
            }
            val kwTokens = skip.keywords.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
            for (kw in kwTokens) {
                skippedKeywordCounts[kw] = (skippedKeywordCounts[kw] ?: 0) + 1
            }
        }

        // 3. Merge, deduplicate, and exclude current / already played tracks
        val candidateMap = mutableMapOf<String, ScoredCandidate>()

        val currentArtistNorm = MusicKeywordExtractor.normalizeArtist(currentTrack.artist)

        fun processCandidate(track: Track, isSourceA: Boolean) {
            val cleanId = track.id.trim()
            if (cleanId.isBlank() || cleanId == currentTrack.id || cleanId in playedHistoryTrackIds) {
                return
            }
            if (!MusicContentFilter.isLikelySong(track)) {
                return
            }

            // De-duplicate by ID or normalized title+artist
            val normTitle = track.title.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
            val normArtist = MusicKeywordExtractor.normalizeArtist(track.artist)
            val dedupeKey = if (cleanId.startsWith("search_res_")) "$normTitle-$normArtist" else cleanId

            if (candidateMap.containsKey(dedupeKey)) return

            // Base score: Source A (Direct YouTube Mix/Related) gets 10.0, Source B gets 7.0
            var score = if (isSourceA) 10.0 else 7.0

            // Diversity Boost: Encourage a diverse mix of artists (not defaulting to an artist-only continuation)
            if (normArtist.isNotBlank() && normArtist != currentArtistNorm) {
                score += 2.5
            }

            // Quality score boost from content filter
            val qualityBonus = (MusicContentFilter.scoreTrackForMusicRelevance(track) - 100).coerceAtLeast(0) / 25.0
            score += qualityBonus

            // Implicit Negative Feedback Penalty from Quick Skips
            var penalty = 0.0
            val artistSkips = skippedArtistCounts[normArtist] ?: 0
            if (artistSkips > 0) {
                // Soft penalty proportional to skip count
                penalty += (artistSkips * 2.0).coerceAtMost(8.0)
            }

            val candidateKeywords = MusicKeywordExtractor.extractKeywords(track.title, track.artist)
            var kwPenalty = 0.0
            for (kw in candidateKeywords) {
                val count = skippedKeywordCounts[kw] ?: 0
                if (count > 0) {
                    kwPenalty += count * 0.75
                }
            }
            penalty += kwPenalty.coerceAtMost(6.0)

            val finalScore = score - penalty
            candidateMap[dedupeKey] = ScoredCandidate(
                track = track,
                score = finalScore,
                source = if (isSourceA) "SourceA-Related" else "SourceB-Search",
                penalty = penalty
            )
        }

        sourceATracks.forEach { processCandidate(it, isSourceA = true) }
        sourceBTracks.forEach { processCandidate(it, isSourceA = false) }

        val rankedCandidates = candidateMap.values.sortedByDescending { it.score }

        Log.i(
            TAG,
            "[RadioEngine:RANKING] Top candidates for radio continuation:\n" +
                    rankedCandidates.take(5).joinToString("\n") {
                        " - [${it.score.format(1)} pts, -${it.penalty.format(1)} pen, ${it.source}] '${it.track.title}' by '${it.track.artist}' (${it.track.id})"
                    }
        )

        val selected = rankedCandidates.firstOrNull()?.track
        if (selected != null) {
            Log.i(TAG, "[RadioEngine:DECISION] Selected next related track: '${selected.title}' by '${selected.artist}' (${selected.id})")
            return@withContext selected
        }

        // Fallback: Pick a curated track not in played history
        val fallback = CuratedTracks.allCurated.firstOrNull { it.id != currentTrack.id && it.id !in playedHistoryTrackIds }
            ?: CuratedTracks.allCurated.first()
        Log.i(TAG, "[RadioEngine:FALLBACK] Using fallback track '${fallback.title}' by '${fallback.artist}'")
        return@withContext fallback
    }

    /**
     * Source A (Primary): Queries NewPipe Extractor for related items / YouTube Mix radio continuation.
     * If NewPipe encounters an issue, falls back to Piped / Invidious stream endpoints.
     */
    private suspend fun fetchSourceARelatedTracks(currentTrack: Track): List<Track> {
        val results = mutableListOf<Track>()
        val cleanId = currentTrack.id.trim()
        if (cleanId.isBlank() || cleanId.startsWith("sample_") || cleanId.startsWith("test_")) {
            return emptyList()
        }

        // 1. Try NewPipe Extractor StreamInfo.relatedItems
        try {
            val url = "https://www.youtube.com/watch?v=$cleanId"
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url)
            val relatedItems = streamInfo.relatedItems ?: emptyList()

            for (item in relatedItems) {
                if (item is StreamInfoItem && MusicContentFilter.isLikelySong(item)) {
                    val vId = extractVideoId(item.url)
                    if (vId.isNotBlank() && vId != cleanId) {
                        results.add(
                            Track(
                                id = vId,
                                title = item.name ?: "Related Song",
                                artist = item.uploaderName ?: "Artist",
                                durationSeconds = item.duration.coerceAtLeast(0L),
                                thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: "https://img.youtube.com/vi/$vId/hqdefault.jpg",
                                addedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            if (results.isNotEmpty()) {
                Log.d(TAG, "NewPipe relatedItems returned ${results.size} valid song tracks for $cleanId")
                return results
            }
        } catch (e: Exception) {
            Log.d(TAG, "NewPipe related stream resolution for $cleanId: ${e.message}")
        }

        // 2. Fallback: Mirror APIs related streams
        try {
            val mirrorResults = fetchRelatedFromMirrors(cleanId)
            if (mirrorResults.isNotEmpty()) {
                return mirrorResults
            }
        } catch (e: Exception) {
            Log.d(TAG, "Mirror related endpoints for $cleanId: ${e.message}")
        }

        return results
    }

    /**
     * Source B (Supplementary): Keyword-based search using terms derived from current track's title and artist.
     */
    private suspend fun fetchSourceBSupplementaryTracks(currentTrack: Track): List<Track> {
        val results = mutableListOf<Track>()
        val cleanArtist = MusicKeywordExtractor.normalizeArtist(currentTrack.artist)
        val cleanKeywords = MusicKeywordExtractor.extractKeywords(currentTrack.title, currentTrack.artist).take(3)

        val queries = mutableListOf<String>()
        if (cleanArtist.isNotBlank()) {
            queries.add("$cleanArtist songs")
            queries.add("$cleanArtist radio mix")
        }
        if (cleanKeywords.isNotEmpty()) {
            queries.add("${cleanKeywords.joinToString(" ")} music")
        }

        for (query in queries) {
            try {
                val searchResult = searchService.searchTracks(query)
                searchResult.getOrNull()?.let { tracks ->
                    results.addAll(tracks.filter { it.id != currentTrack.id })
                }
            } catch (e: Exception) {
                Log.d(TAG, "Supplementary search failed for '$query': ${e.message}")
            }
        }

        return results
    }

    private fun fetchRelatedFromMirrors(videoId: String): List<Track> {
        val endpoints = listOf(
            "https://pipedapi.kavin.rocks/streams/$videoId",
            "https://api.piped.privacy.com.de/streams/$videoId",
            "https://invidious.nerdvpn.de/api/v1/videos/$videoId"
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "SWARMusicPlayer/1.0 (Android)")

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val tracks = mutableListOf<Track>()

                    val relatedArray = json.optJSONArray("relatedStreams")
                        ?: json.optJSONArray("recommendedVideos")

                    if (relatedArray != null) {
                        for (i in 0 until relatedArray.length()) {
                            val item = relatedArray.getJSONObject(i)
                            val itemUrl = item.optString("url", "")
                            val vId = if (item.has("videoId")) item.getString("videoId") else extractVideoId(itemUrl)
                            val title = item.optString("title", "")
                            val artist = item.optString("uploaderName", item.optString("author", ""))
                            val duration = item.optLong("duration", item.optLong("lengthSeconds", 0L))

                            if (vId.isNotBlank() && vId != videoId && MusicContentFilter.isLikelySong(title, artist, duration)) {
                                tracks.add(
                                    Track(
                                        id = vId,
                                        title = title,
                                        artist = artist,
                                        durationSeconds = duration,
                                        thumbnailUrl = item.optString("thumbnail", "https://img.youtube.com/vi/$vId/hqdefault.jpg"),
                                        addedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        return tracks
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Mirror endpoint $endpoint failed: ${e.message}")
            }
        }
        return emptyList()
    }

    private fun extractVideoId(input: String): String {
        val trimmed = input.trim()
        if (!trimmed.contains("/") && !trimmed.contains("?")) {
            return trimmed
        }
        return try {
            val uri = android.net.Uri.parse(trimmed)
            when {
                uri.host?.contains("youtu.be") == true -> uri.lastPathSegment ?: trimmed
                uri.getQueryParameter("v") != null -> uri.getQueryParameter("v") ?: trimmed
                uri.pathSegments.contains("shorts") -> uri.lastPathSegment ?: trimmed
                uri.pathSegments.contains("embed") -> uri.lastPathSegment ?: trimmed
                else -> uri.lastPathSegment ?: trimmed
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    private fun Double.format(digits: Int): String = "%.${digits}f".format(Locale.ROOT, this)

    private data class ScoredCandidate(
        val track: Track,
        val score: Double,
        val source: String,
        val penalty: Double
    )
}
