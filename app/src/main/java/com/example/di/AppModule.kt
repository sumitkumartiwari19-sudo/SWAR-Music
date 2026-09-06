package com.example.di

import android.content.Context
import com.example.data.local.SwarDatabase
import com.example.data.preferences.SettingsPreferences
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.DownloadsRepository
import com.example.data.repository.DownloadsRepositoryImpl
import com.example.data.repository.HistoryAndQueueRepository
import com.example.data.repository.HistoryAndQueueRepositoryImpl
import com.example.data.repository.LikedSongsRepository
import com.example.data.repository.LikedSongsRepositoryImpl
import com.example.data.repository.PlayerRepository
import com.example.data.repository.PlayerRepositoryImpl
import com.example.data.repository.PlaylistsRepository
import com.example.data.repository.PlaylistsRepositoryImpl
import com.example.playback.PlaybackManager
import com.example.playback.extractor.AudioStreamResolver
import com.example.playback.extractor.PlaylistExtractor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Dependency Injection Module for SWAR Music.
 * Provides singleton instances of Room Database, Repositories, and Preferences.
 */
object AppModule {
    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var database: SwarDatabase? = null

    @Volatile
    private var themePreferences: ThemePreferences? = null

    @Volatile
    private var settingsPreferences: SettingsPreferences? = null

    @Volatile
    private var authRepository: AuthRepository? = null

    @Volatile
    private var likedSongsRepository: LikedSongsRepository? = null

    @Volatile
    private var playlistsRepository: PlaylistsRepository? = null

    @Volatile
    private var historyAndQueueRepository: HistoryAndQueueRepository? = null

    @Volatile
    private var downloadsRepository: DownloadsRepository? = null

    @Volatile
    private var audioStreamResolver: AudioStreamResolver? = null

    @Volatile
    private var playlistExtractor: PlaylistExtractor? = null

    @Volatile
    private var playbackManager: PlaybackManager? = null

    @Volatile
    private var playerRepository: PlayerRepository? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getContext(): Context {
        return appContext ?: throw IllegalStateException("AppModule not initialized with Context")
    }

    fun provideDatabase(context: Context = getContext()): SwarDatabase {
        return database ?: synchronized(this) {
            database ?: SwarDatabase.getInstance(context.applicationContext).also {
                database = it
            }
        }
    }

    fun provideSettingsPreferences(context: Context = getContext()): SettingsPreferences {
        return settingsPreferences ?: synchronized(this) {
            settingsPreferences ?: SettingsPreferences(context.applicationContext).also {
                settingsPreferences = it
            }
        }
    }

    fun provideThemePreferences(context: Context = getContext()): ThemePreferences {
        return themePreferences ?: synchronized(this) {
            themePreferences ?: ThemePreferences(context.applicationContext).also {
                themePreferences = it
            }
        }
    }

    fun provideFirebaseAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            android.util.Log.w("AppModule", "FirebaseAuth instance unavailable: ${e.message}")
            null
        }
    }

    fun provideFirebaseFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            android.util.Log.w("AppModule", "FirebaseFirestore instance unavailable: ${e.message}")
            null
        }
    }

    fun provideAuthRepository(): AuthRepository {
        return authRepository ?: synchronized(this) {
            authRepository ?: AuthRepositoryImpl(
                auth = provideFirebaseAuth(),
                firestore = provideFirebaseFirestore()
            ).also { authRepository = it }
        }
    }

    fun provideLikedSongsRepository(context: Context? = appContext): LikedSongsRepository {
        return likedSongsRepository ?: synchronized(this) {
            likedSongsRepository ?: LikedSongsRepositoryImpl(
                database = if (context != null) provideDatabase(context) else database ?: throw IllegalStateException("Context required")
            ).also { likedSongsRepository = it }
        }
    }

    fun providePlaylistsRepository(context: Context? = appContext): PlaylistsRepository {
        return playlistsRepository ?: synchronized(this) {
            playlistsRepository ?: PlaylistsRepositoryImpl(
                database = if (context != null) provideDatabase(context) else database ?: throw IllegalStateException("Context required")
            ).also { playlistsRepository = it }
        }
    }

    fun provideHistoryAndQueueRepository(context: Context? = appContext): HistoryAndQueueRepository {
        return historyAndQueueRepository ?: synchronized(this) {
            historyAndQueueRepository ?: HistoryAndQueueRepositoryImpl(
                database = if (context != null) provideDatabase(context) else database ?: throw IllegalStateException("Context required")
            ).also { historyAndQueueRepository = it }
        }
    }

    fun provideDownloadsRepository(context: Context = getContext()): DownloadsRepository {
        return downloadsRepository ?: synchronized(this) {
            downloadsRepository ?: DownloadsRepositoryImpl(
                context = context.applicationContext
            ).also { downloadsRepository = it }
        }
    }

    fun provideAudioStreamResolver(): AudioStreamResolver {
        return audioStreamResolver ?: synchronized(this) {
            audioStreamResolver ?: AudioStreamResolver().also { audioStreamResolver = it }
        }
    }

    fun providePlaylistExtractor(): PlaylistExtractor {
        return playlistExtractor ?: synchronized(this) {
            playlistExtractor ?: PlaylistExtractor().also { playlistExtractor = it }
        }
    }

    fun providePlaybackManager(context: Context = getContext()): PlaybackManager {
        return playbackManager ?: synchronized(this) {
            playbackManager ?: PlaybackManager(
                context = context.applicationContext,
                streamResolver = provideAudioStreamResolver(),
                authRepository = provideAuthRepository(),
                historyAndQueueRepository = provideHistoryAndQueueRepository(context),
                downloadsRepository = provideDownloadsRepository(context),
                quickSkipDao = provideDatabase(context).quickSkipDao()
            ).also { playbackManager = it }
        }
    }

    fun providePlayerRepository(context: Context = getContext()): PlayerRepository {
        return playerRepository ?: synchronized(this) {
            playerRepository ?: PlayerRepositoryImpl(
                playbackManager = providePlaybackManager(context)
            ).also { playerRepository = it }
        }
    }
}
