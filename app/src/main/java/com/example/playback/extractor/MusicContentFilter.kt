package com.example.playback.extractor

import android.util.Log
import com.example.data.model.Track
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Locale

/**
 * Shared filtering, ranking, and scoring engine for Indian & YouTube Music streams.
 * Ensures that Home recommendations and Search results contain ONLY individual music tracks,
 * excluding full movies, trailers, vlogs, podcasts, and non-music long-form videos.
 */
object MusicContentFilter {
    private const val TAG = "MusicContentFilter"

    // Duration boundaries (in seconds)
    const val MIN_SONG_DURATION_SECONDS = 45L       // 45 seconds (filters short previews, snippets)
    const val MAX_SONG_DURATION_SECONDS = 780L      // 13 minutes (covers extended classical/fusion tracks & singles)
    const val MAX_JUKEBOX_DURATION_SECONDS = 1800L  // 30 minutes (only allowed if explicitly jukebox/compilation)

    // Hard disqualifiers - any item with these terms in the title is definitively NOT an individual song
    private val TITLE_HARD_BLOCKLIST = listOf(
        "full movie",
        "entire movie",
        "full film",
        "short film",
        "watch full movie",
        "watch movie",
        "hindi movie",
        "tamil movie",
        "telugu movie",
        "malayalam movie",
        "kannada movie",
        "punjabi movie",
        "bengali movie",
        "south movie",
        "bhojpuri movie",
        "english movie",
        "hollywood movie",
        "official trailer",
        "trailer",
        "teaser",
        "promo teaser",
        "motion poster",
        "first look",
        "glimpse",
        "title glimpse",
        "sneak peek",
        "full episode",
        "episode",
        "ep ",
        "ep.",
        "web series",
        "podcast",
        "vlog",
        "interview",
        "press meet",
        "press conference",
        "audio launch event",
        "success meet",
        "pre release event",
        "trailer launch",
        "reaction",
        "movie review",
        "honest review",
        "trailer review",
        "teaser review",
        "public talk",
        "public response",
        "theatre response",
        "behind the scenes",
        "making of",
        "making video",
        "bloopers",
        "deleted scenes",
        "movie scenes",
        "comedy scene",
        "fight scene",
        "action scene",
        "climax scene",
        "romantic scene",
        "dialogue promo",
        "gameplay",
        "walkthrough",
        "telefilm",
        "box office collection",
        "unboxing",
        "tutorial",
        "how to"
    )

    // Multi-part movie patterns e.g. "Part 1", "Part 2", "Part-1", etc.
    private val PART_REGEX = Regex("""\b(part|pt)\s*[-_:/]?\s*[0-9]+\b""", RegexOption.IGNORE_CASE)

    // Known major Indian & global music label channels
    private val MUSIC_LABELS = listOf(
        "t-series",
        "zee music",
        "sony music",
        "saregama",
        "yrf",
        "tips official",
        "speed records",
        "aditya music",
        "lahari music",
        "svf music",
        "eros now",
        "white hill",
        "think music",
        "venus",
        "times music",
        "geet mp3",
        "desi music factory",
        "vyrloriginals",
        "t-series regional",
        "t-series telugu",
        "t-series tamil",
        "anand audio",
        "muzik247",
        "manorama music",
        "saregama regional",
        "warner music",
        "universal music",
        "coke studio",
        "mtv india",
        "dhvani bhanushali",
        "arijit singh",
        "diljit dosanjh",
        "karan aujla",
        "shreya ghoshal",
        "armaan malik",
        "badshah",
        "sidhu moose wala"
    )

    // Positive indicators that strongly suggest this item is a song
    private val SONG_POSITIVE_KEYWORDS = listOf(
        "song",
        "audio",
        "video song",
        "lyric",
        "lyrics",
        "lyrical",
        "music",
        "remix",
        "lofi",
        "lo-fi",
        "slowed",
        "reverb",
        "acoustic",
        "unplugged",
        "cover",
        "live performance",
        "official audio",
        "official video",
        "official music video",
        "raga",
        "raag",
        "sitar",
        "flute",
        "tabla",
        "ghazal",
        "qawwali",
        "instrumental",
        "soundtrack",
        "ost",
        "singles"
    )

    /**
     * Determines whether a track is likely a legitimate song track.
     */
    fun isLikelySong(
        title: String,
        artist: String,
        durationSeconds: Long,
        channelName: String? = null
    ): Boolean {
        val lowerTitle = title.lowercase(Locale.ROOT).trim()
        val lowerArtist = (channelName ?: artist).lowercase(Locale.ROOT).trim()

        if (lowerTitle.isBlank()) return false

        // 1. Duration Check
        // If duration is 0 or negative (unknown live stream or metadata omitted by search), we let keyword heuristics decide.
        if (durationSeconds > 0) {
            val isJukebox = lowerTitle.contains("jukebox") || lowerTitle.contains("all songs") || lowerTitle.contains("mashup")
            val maxAllowed = if (isJukebox) MAX_JUKEBOX_DURATION_SECONDS else MAX_SONG_DURATION_SECONDS

            if (durationSeconds < MIN_SONG_DURATION_SECONDS) {
                // Too short - likely a ringtone, teaser, or clip
                return false
            }
            if (durationSeconds > maxAllowed) {
                // Too long - full movie (2-3 hrs), full podcast, or long recording
                return false
            }
        }

        // 2. Hard Blocklist Check on Title
        for (blockedTerm in TITLE_HARD_BLOCKLIST) {
            if (lowerTitle.contains(blockedTerm)) {
                // Special check: if title contains "song from movie" or "(from 'movie name')", it's a song
                val isSongException = (blockedTerm == "trailer" || blockedTerm == "teaser" || blockedTerm == "hindi movie" || blockedTerm == "south movie") &&
                        (lowerTitle.contains("audio song") || lowerTitle.contains("video song") || lowerTitle.contains("full song") || lowerTitle.contains("lyrical song"))
                
                if (!isSongException) {
                    return false
                }
            }
        }

        // 3. Multi-part movie regex check (e.g. "Movie Name Part 1")
        if (PART_REGEX.containsMatchIn(lowerTitle)) {
            // Unless it is explicitly "Part 1 (Song)" or "Song Part 1"
            if (!lowerTitle.contains("song") && !lowerTitle.contains("audio") && !lowerTitle.contains("music")) {
                return false
            }
        }

        // 4. Check for standalone "movie" in title without song keywords
        if (lowerTitle.contains("movie") || lowerTitle.contains("cinema")) {
            val hasSongCue = SONG_POSITIVE_KEYWORDS.any { lowerTitle.contains(it) } ||
                    lowerTitle.contains("from ") || lowerTitle.contains("feat") || lowerTitle.contains("ft.") || lowerTitle.contains("|")
            if (!hasSongCue) {
                return false
            }
        }

        // Passed all negative filters
        return true
    }

    /**
     * Overload for Track model
     */
    fun isLikelySong(track: Track): Boolean {
        return isLikelySong(
            title = track.title,
            artist = track.artist,
            durationSeconds = track.durationSeconds
        )
    }

    /**
     * Overload for NewPipe StreamInfoItem
     */
    fun isLikelySong(item: StreamInfoItem): Boolean {
        return isLikelySong(
            title = item.name ?: "",
            artist = item.uploaderName ?: "",
            durationSeconds = item.duration.coerceAtLeast(0L),
            channelName = item.uploaderName
        )
    }

    /**
     * Scores a track to rank official YouTube Music / Topic and verified Music Label releases above generic uploads.
     */
    fun scoreTrackForMusicRelevance(track: Track, userQuery: String? = null): Int {
        var score = 100
        val lowerTitle = track.title.lowercase(Locale.ROOT)
        val lowerArtist = track.artist.lowercase(Locale.ROOT)

        // 1. YouTube Music Topic Channel boost
        if (lowerArtist.endsWith("- topic") || lowerArtist.contains("topic")) {
            score += 60
        }

        // 2. Verified Music Labels boost
        if (MUSIC_LABELS.any { lowerArtist.contains(it) }) {
            score += 45
        }

        // 3. Positive Song keyword boost
        if (lowerTitle.contains("official music video") || lowerTitle.contains("official audio")) {
            score += 30
        } else if (lowerTitle.contains("lyrical") || lowerTitle.contains("lyrics") || lowerTitle.contains("video song")) {
            score += 20
        } else if (SONG_POSITIVE_KEYWORDS.any { lowerTitle.contains(it) }) {
            score += 15
        }

        // 4. Ideal Song Duration window (2.5 - 6 minutes is prime for Indian film/pop music)
        if (track.durationSeconds in 150..360) {
            score += 20
        }

        // 5. Query relevance boost
        if (!userQuery.isNullOrBlank()) {
            val queryWords = userQuery.lowercase(Locale.ROOT).split(" ").filter { it.length > 2 }
            val matchingWords = queryWords.count { lowerTitle.contains(it) || lowerArtist.contains(it) }
            score += (matchingWords * 15)
        }

        return score
    }

    /**
     * Filters an input list of tracks using [isLikelySong] and ranks them by musical quality score.
     */
    fun filterAndRankSongs(tracks: List<Track>, userQuery: String? = null): List<Track> {
        val songOnly = tracks.filter { isLikelySong(it) }
        val distinctTracks = songOnly.distinctBy { it.id }
        return distinctTracks.sortedByDescending { scoreTrackForMusicRelevance(it, userQuery) }
    }
}
