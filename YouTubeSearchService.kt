package com.example.playback.extractor

import android.util.Log
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Service to execute YouTube searches using NewPipe Extractor with fallback to Invidious/Piped APIs.
 * Applies [MusicContentFilter] to filter out movies, trailers, vlogs, and podcasts,
 * and performs movie-name song biasing to ensure only individual music tracks are surfaced.
 */
class YouTubeSearchService(
    private val downloader: NewPipeDownloaderImpl = NewPipeDownloaderImpl.getInstance()
) {
    companion object {
        private const val TAG = "YouTubeSearchService"
    }

    init {
        NewPipeDownloaderImpl.initGlobal()
    }

    /**
     * Searches for music tracks with music-biased query expansion and filtering.
     * When a user searches a movie or artist name (e.g. "Raja" or "Pathaan"),
     * queries for songs/audio tracks are also searched in parallel, then filtered and de-duplicated.
     */
    suspend fun searchTracks(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        val lowerQuery = trimmed.lowercase(Locale.ROOT)
        val queriesToSearch = mutableListOf<String>()
        queriesToSearch.add(trimmed)

        // If user didn't explicitly include music keywords, append music variants to surface movie songs
        val hasMusicKeyword = lowerQuery.contains("song") || lowerQuery.contains("audio") ||
                lowerQuery.contains("music") || lowerQuery.contains("track") ||
                lowerQuery.contains("lofi") || lowerQuery.contains("raga") ||
                lowerQuery.contains("jukebox") || lowerQuery.contains("remix")

        if (!hasMusicKeyword) {
            queriesToSearch.add("$trimmed songs")
            queriesToSearch.add("$trimmed all songs")
        }

        Log.d(TAG, "Initiating music-filtered search for queries: $queriesToSearch")

        // Execute searches in parallel
        val deferredResults = queriesToSearch.map { subQuery ->
            async {
                executeSingleSearch(subQuery)
            }
        }

        val allResults = deferredResults.awaitAll()
        val combinedTracks = mutableListOf<Track>()
        val allErrors = mutableListOf<String>()

        for (res in allResults) {
            res.fold(
                onSuccess = { tracks -> combinedTracks.addAll(tracks) },
                onFailure = { err -> err.message?.let { allErrors.add(it) } }
            )
        }

        // Filter and rank through shared MusicContentFilter
        val filteredAndRanked = MusicContentFilter.filterAndRankSongs(combinedTracks, trimmed)

        if (filteredAndRanked.isNotEmpty()) {
            Log.d(TAG, "Search for '$trimmed' yielded ${filteredAndRanked.size} valid song tracks after music filtering")
            return@withContext Result.success(filteredAndRanked)
        }

        if (allErrors.isNotEmpty()) {
            return@withContext Result.failure(IOException(allErrors.joinToString(" | ")))
        }

        return@withContext Result.success(emptyList())
    }

    /**
     * Executes a single search for a specific query string.
     */
    private suspend fun executeSingleSearch(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext Result.success(emptyList())

        val errors = mutableListOf<String>()

        // 1. Try NewPipe Extractor direct search
        try {
            Log.d(TAG, "Executing NewPipe search query: '$trimmed'")
            val searchQHFactory = ServiceList.YouTube.searchQHFactory
            val searchQueryHandler = searchQHFactory.fromQuery(trimmed)
            val searchInfo = SearchInfo.getInfo(ServiceList.YouTube, searchQueryHandler)

            val relatedItems = searchInfo.relatedItems ?: emptyList()
            val extractedTracks = relatedItems.mapNotNull { item ->
                if (item is StreamInfoItem && MusicContentFilter.isLikelySong(item)) {
                    val videoId = extractVideoId(item.url)
                    Track(
                        id = if (videoId.isNotBlank()) videoId else item.url,
                        title = item.name ?: "Unknown Song",
                        artist = item.uploaderName ?: "Unknown Artist",
                        durationSeconds = item.duration.coerceAtLeast(0L),
                        thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                        addedAt = System.currentTimeMillis()
                    )
                } else null
            }

            if (extractedTracks.isNotEmpty()) {
                Log.d(TAG, "NewPipe search succeeded with ${extractedTracks.size} results for '$trimmed'")
                return@withContext Result.success(extractedTracks)
            } else {
                errors.add("NewPipe direct search returned 0 items")
            }
        } catch (e: Exception) {
            val errMessage = "NewPipe search failed [${e.javaClass.simpleName}]: ${e.message}"
            Log.e(TAG, errMessage, e)
            errors.add(errMessage)
        }

        // 2. Fallback: Search via Piped / Invidious public mirror APIs
        try {
            Log.d(TAG, "Attempting mirror API search fallback for '$trimmed'")
            val mirrorResults = searchViaMirrors(trimmed)
            val filteredMirrors = mirrorResults.filter { MusicContentFilter.isLikelySong(it) }
            if (filteredMirrors.isNotEmpty()) {
                Log.d(TAG, "Mirror search succeeded with ${filteredMirrors.size} results for '$trimmed'")
                return@withContext Result.success(filteredMirrors)
            } else {
                errors.add("Mirror search returned 0 items")
            }
        } catch (e: Exception) {
            val errMessage = "Mirror search failed [${e.javaClass.simpleName}]: ${e.message}"
            Log.e(TAG, errMessage, e)
            errors.add(errMessage)
        }

        // 3. Fallback: YouTube Suggest/Search scraping fallback
        try {
            val suggestResults = searchViaYouTubeSuggest(trimmed)
            val filteredSuggest = suggestResults.filter { MusicContentFilter.isLikelySong(it) }
            if (filteredSuggest.isNotEmpty()) {
                return@withContext Result.success(filteredSuggest)
            }
        } catch (e: Exception) {
            errors.add("Suggest search failed: ${e.message}")
        }

        val combinedErrors = errors.joinToString(" | ")
        Result.failure(IOException("Search failed for '$trimmed': $combinedErrors"))
    }

    private fun searchViaMirrors(query: String): List<Track> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val endpoints = listOf(
            "https://pipedapi.kavin.rocks/search?q=$encoded&filter=music_songs",
            "https://api.piped.privacy.com.de/search?q=$encoded&filter=music_songs",
            "https://invidious.nerdvpn.de/api/v1/search?q=$encoded&type=video",
            "https://inv.tux.pizza/api/v1/search?q=$encoded&type=video"
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "SWARMusicPlayer/1.0 (Android)")

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val results = mutableListOf<Track>()

                    if (text.startsWith("[")) {
                        // Invidious array format
                        val jsonArray = JSONArray(text)
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val videoId = item.optString("videoId")
                            val title = item.optString("title", "Unknown Title")
                            val artist = item.optString("author", "Unknown Artist")
                            val duration = item.optLong("lengthSeconds", 0L)

                            if (videoId.isNotBlank() && MusicContentFilter.isLikelySong(title, artist, duration)) {
                                results.add(
                                    Track(
                                        id = videoId,
                                        title = title,
                                        artist = artist,
                                        durationSeconds = duration,
                                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                                        addedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    } else if (text.startsWith("{")) {
                        // Piped object format {"items": [...]}
                        val json = JSONObject(text)
                        val items = json.optJSONArray("items")
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val itemUrl = item.optString("url", "")
                                val videoId = extractVideoId(itemUrl)
                                val title = item.optString("title", "Unknown Title")
                                val artist = item.optString("uploaderName", "Unknown Artist")
                                val duration = item.optLong("duration", 0L)

                                if (videoId.isNotBlank() && MusicContentFilter.isLikelySong(title, artist, duration)) {
                                    results.add(
                                        Track(
                                            id = videoId,
                                            title = title,
                                            artist = artist,
                                            durationSeconds = duration,
                                            thumbnailUrl = item.optString("thumbnail", "https://img.youtube.com/vi/$videoId/hqdefault.jpg"),
                                            addedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (results.isNotEmpty()) {
                        return results
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Endpoint $endpoint failed: ${e.message}")
            }
        }
        return emptyList()
    }

    private fun searchViaYouTubeSuggest(query: String): List<Track> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=$encoded")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")

        if (conn.responseCode == 200) {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val startIdx = text.indexOf("([")
            val endIdx = text.lastIndexOf("])")
            if (startIdx != -1 && endIdx != -1) {
                val jsonStr = text.substring(startIdx + 1, endIdx + 1)
                val array = JSONArray(jsonStr)
                if (array.length() > 1) {
                    val suggestions = array.getJSONArray(1)
                    val tracks = mutableListOf<Track>()
                    for (i in 0 until suggestions.length()) {
                        val suggestionItem = suggestions.getJSONArray(i)
                        val title = suggestionItem.getString(0)
                        if (MusicContentFilter.isLikelySong(title, "YouTube Music Result", 210L)) {
                            tracks.add(
                                Track(
                                    id = "search_res_${i}_${System.currentTimeMillis()}",
                                    title = title,
                                    artist = "YouTube Music Result",
                                    durationSeconds = 210,
                                    thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
                                    addedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    return tracks
                }
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
}

