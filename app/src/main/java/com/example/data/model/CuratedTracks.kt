package com.example.data.model

/**
 * Curated offline-ready default Indian music tracks used for instant cold-start
 * and reliable offline fallback matching YouTube Music standards.
 */
object CuratedTracks {
    val quickPicks = listOf(
        Track(
            id = "BddP6PYo2gs",
            title = "Kesariya (From 'Brahmastra')",
            artist = "Arijit Singh, Pritam, Amitabh Bhattacharya",
            durationSeconds = 268,
            thumbnailUrl = "https://img.youtube.com/vi/BddP6PYo2gs/hqdefault.jpg"
        ),
        Track(
            id = "VAdGW7QDJhU",
            title = "Chaleya (From 'Jawan')",
            artist = "Arijit Singh, Shilpa Rao, Anirudh Ravichander",
            durationSeconds = 200,
            thumbnailUrl = "https://img.youtube.com/vi/VAdGW7QDJhU/hqdefault.jpg"
        ),
        Track(
            id = "ElZfdU54Cp8",
            title = "Apna Bana Le (From 'Bhediya')",
            artist = "Arijit Singh, Sachin-Jigar",
            durationSeconds = 261,
            thumbnailUrl = "https://img.youtube.com/vi/ElZfdU54Cp8/hqdefault.jpg"
        ),
        Track(
            id = "IJq0cvOcfU8",
            title = "Tum Hi Ho (From 'Aashiqui 2')",
            artist = "Arijit Singh, Mithoon",
            durationSeconds = 262,
            thumbnailUrl = "https://img.youtube.com/vi/IJq0cvOcfU8/hqdefault.jpg"
        ),
        Track(
            id = "jfKfPfyJRdk",
            title = "Monsoon Lo-Fi Coffee Chill",
            artist = "SWAR Acoustic Lounge",
            durationSeconds = 214,
            thumbnailUrl = "https://img.youtube.com/vi/jfKfPfyJRdk/hqdefault.jpg"
        ),
        Track(
            id = "kJQP7kiw5Fk",
            title = "Raga Darbari Kanara (Night)",
            artist = "Pandit Shivendra & Fusion Ensemble",
            durationSeconds = 372,
            thumbnailUrl = "https://img.youtube.com/vi/kJQP7kiw5Fk/hqdefault.jpg"
        )
    )

    val trendingNow = listOf(
        Track(
            id = "RLzC55ai0eo",
            title = "Heeriye (feat. Arijit Singh)",
            artist = "Jasleen Royal, Arijit Singh, Dulquer Salmaan",
            durationSeconds = 194,
            thumbnailUrl = "https://img.youtube.com/vi/RLzC55ai0eo/hqdefault.jpg"
        ),
        Track(
            id = "TaubaTaubaTrackId",
            title = "Tauba Tauba (From 'Bad Newz')",
            artist = "Karan Aujla",
            durationSeconds = 207,
            thumbnailUrl = "https://img.youtube.com/vi/v9K0uA_R5d4/hqdefault.jpg"
        ),
        Track(
            id = "1eS9B0V1-e0",
            title = "Maan Meri Jaan",
            artist = "King",
            durationSeconds = 196,
            thumbnailUrl = "https://img.youtube.com/vi/VUwhXms6wHg/hqdefault.jpg"
        ),
        Track(
            id = "cl0a3i2wFcc",
            title = "Kahani Suno 2.0",
            artist = "Kaifi Khalil",
            durationSeconds = 173,
            thumbnailUrl = "https://img.youtube.com/vi/_XBVWlI8TsQ/hqdefault.jpg"
        ),
        Track(
            id = "5Eqb_-j3FDA",
            title = "Pasoori",
            artist = "Ali Sethi, Shae Gill (Coke Studio)",
            durationSeconds = 224,
            thumbnailUrl = "https://img.youtube.com/vi/5Eqb_-j3FDA/hqdefault.jpg"
        ),
        Track(
            id = "b4e85F_XWcQ",
            title = "Midnight Raag Yaman Fusion",
            artist = "Amjad Ali Strings & Modern Synth",
            durationSeconds = 328,
            thumbnailUrl = "https://img.youtube.com/vi/b4e85F_XWcQ/hqdefault.jpg"
        )
    )

    val newReleasesAndRegional = listOf(
        Track(
            id = "HukumTrackId01",
            title = "Hukum - Thalaivar Alappara (From 'Jailer')",
            artist = "Anirudh Ravichander",
            durationSeconds = 207,
            thumbnailUrl = "https://img.youtube.com/vi/1F3hm6MfR1k/hqdefault.jpg"
        ),
        Track(
            id = "IlluminatiTrackId02",
            title = "Illuminati (From 'Aavesham')",
            artist = "Sushin Shyam, Dabzee",
            durationSeconds = 193,
            thumbnailUrl = "https://img.youtube.com/vi/tOM-nWPcR4U/hqdefault.jpg"
        ),
        Track(
            id = "PehleBhiMain03",
            title = "Pehle Bhi Main (From 'ANIMAL')",
            artist = "Vishal Mishra, Raj Shekhar",
            durationSeconds = 250,
            thumbnailUrl = "https://img.youtube.com/vi/2v8r_rD3D60/hqdefault.jpg"
        ),
        Track(
            id = "GuliMataTrackId04",
            title = "Guli Mata",
            artist = "Saad Lamjarred, Shreya Ghoshal, Rajat Nagpal",
            durationSeconds = 217,
            thumbnailUrl = "https://img.youtube.com/vi/p8f8XpWl03Y/hqdefault.jpg"
        )
    )

    val allCurated = quickPicks + trendingNow + newReleasesAndRegional
}
