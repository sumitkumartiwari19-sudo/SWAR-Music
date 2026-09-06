package com.example.playback.extractor

import java.util.Locale

/**
 * Extracts normalized keywords and tags from music metadata (title, artist)
 * for similarity matching and implicit quick-skip penalty scoring.
 */
object MusicKeywordExtractor {

    private val STOP_WORDS = setOf(
        "the", "a", "an", "and", "or", "in", "on", "at", "to", "for", "of", "with", "by", "from",
        "official", "video", "audio", "lyrics", "lyric", "hd", "4k", "hq", "remix", "song", "songs",
        "music", "full", "feat", "ft", "prod", "version", "original", "live", "track", "records",
        "series", "entertainment", "presents", "starring", "directed", "composed", "singer", "singers",
        "movie", "film", "teaser", "trailer", "release", "studio", "channel", "topic", "status",
        "mp3", "flac", "wav", "m4a", "youtube", "music", "vevo"
    )

    /**
     * Extracts a set of clean, lowercased, meaningful keyword tokens from track title and artist.
     */
    fun extractKeywords(title: String, artist: String): Set<String> {
        val raw = "$title $artist".lowercase(Locale.ROOT)
        // Clean out brackets, punctuation, special symbols
        val cleaned = raw.replace(Regex("""[\[\]\(\)\{\}\-–—_:\.,!?'"|/\\+*&^%$#@~`<>]"""), " ")
        return cleaned.split(Regex("""\s+"""))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in STOP_WORDS && !it.all { ch -> ch.isDigit() } }
            .toSet()
    }

    /**
     * Extracts primary artist name tokens (e.g., "Arijit Singh" -> ["arijit", "singh"]).
     */
    fun extractArtistTokens(artist: String): Set<String> {
        val cleaned = artist.lowercase(Locale.ROOT)
            .replace("- topic", "")
            .replace("topic", "")
            .replace("official", "")
            .replace(Regex("""[\[\]\(\)\{\}\-–—_:\.,!?'"|/\\+*&^%$#@~`<>]"""), " ")
        return cleaned.split(Regex("""\s+"""))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in STOP_WORDS }
            .toSet()
    }

    /**
     * Normalizes an artist name for exact or substring comparisons.
     */
    fun normalizeArtist(artist: String): String {
        return artist.lowercase(Locale.ROOT)
            .replace("- topic", "")
            .replace("topic", "")
            .replace("official", "")
            .replace("vevo", "")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")
    }
}
