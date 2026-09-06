package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.playback.VideoQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "swar_settings_prefs")

enum class FontSizeScale(val scaleFactor: Float, val label: String) {
    SMALL(0.85f, "Small"),
    DEFAULT(1.0f, "Default"),
    LARGE(1.15f, "Large")
}

enum class AudioQuality(val label: String, val bitrateKbps: Int) {
    AUTO("Auto", 0),
    HIGH("High (320 kbps)", 320),
    DATA_SAVER("Data Saver (96 kbps)", 96)
}

enum class DownloadAudioQuality(val label: String, val bitrateKbps: Int) {
    HIGH("High (320 kbps)", 320),
    MEDIUM("Medium (160 kbps)", 160)
}

enum class NotificationControlsStyle(val label: String) {
    FULL("Full Controls"),
    MINIMAL("Minimal")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSizeScale: FontSizeScale = FontSizeScale.DEFAULT,
    val highContrastMode: Boolean = false,
    val defaultAudioQuality: AudioQuality = AudioQuality.AUTO,
    val defaultVideoQuality: VideoQuality = VideoQuality.AUTO,
    val gaplessPlayback: Boolean = true,
    val crossfadeDurationSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val audioNormalization: Boolean = false,
    val autoplaySimilar: Boolean = true,
    val resumePlaybackOnOpen: Boolean = true,
    val skipSilence: Boolean = false,
    val downloadAudioQuality: DownloadAudioQuality = DownloadAudioQuality.HIGH,
    val downloadOnlyOnWifi: Boolean = true,
    val mobileStreamingQuality: AudioQuality = AudioQuality.DATA_SAVER,
    val wifiStreamingQuality: AudioQuality = AudioQuality.HIGH,
    val notificationControlsStyle: NotificationControlsStyle = NotificationControlsStyle.FULL,
    val personalizedRecommendations: Boolean = true
)

class SettingsPreferences(private val context: Context) {
    private val dataStore = context.settingsDataStore

    companion object {
        // Appearance
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_FONT_SIZE_SCALE = stringPreferencesKey("font_size_scale")
        val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast_mode")

        // Playback
        val KEY_DEFAULT_AUDIO_QUALITY = stringPreferencesKey("default_audio_quality")
        val KEY_DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val KEY_GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val KEY_CROSSFADE_SECONDS = intPreferencesKey("crossfade_duration_seconds")
        val KEY_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val KEY_AUDIO_NORMALIZATION = booleanPreferencesKey("audio_normalization")
        val KEY_AUTOPLAY_SIMILAR = booleanPreferencesKey("autoplay_similar")
        val KEY_RESUME_PLAYBACK = booleanPreferencesKey("resume_playback_on_open")
        val KEY_SKIP_SILENCE = booleanPreferencesKey("skip_silence")

        // Downloads
        val KEY_DOWNLOAD_AUDIO_QUALITY = stringPreferencesKey("download_audio_quality")
        val KEY_DOWNLOAD_ONLY_ON_WIFI = booleanPreferencesKey("download_only_on_wifi")

        // Data Usage
        val KEY_MOBILE_STREAMING_QUALITY = stringPreferencesKey("mobile_streaming_quality")
        val KEY_WIFI_STREAMING_QUALITY = stringPreferencesKey("wifi_streaming_quality")

        // Notifications
        val KEY_NOTIFICATION_STYLE = stringPreferencesKey("notification_controls_style")

        // Privacy
        val KEY_PERSONALIZED_RECOMMENDATIONS = booleanPreferencesKey("personalized_recommendations")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val themeMode = try {
            ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val fontScale = try {
            FontSizeScale.valueOf(prefs[KEY_FONT_SIZE_SCALE] ?: FontSizeScale.DEFAULT.name)
        } catch (_: Exception) {
            FontSizeScale.DEFAULT
        }

        val defaultAudio = try {
            AudioQuality.valueOf(prefs[KEY_DEFAULT_AUDIO_QUALITY] ?: AudioQuality.AUTO.name)
        } catch (_: Exception) {
            AudioQuality.AUTO
        }

        val defaultVideo = try {
            VideoQuality.valueOf(prefs[KEY_DEFAULT_VIDEO_QUALITY] ?: VideoQuality.AUTO.name)
        } catch (_: Exception) {
            VideoQuality.AUTO
        }

        val downloadQuality = try {
            DownloadAudioQuality.valueOf(prefs[KEY_DOWNLOAD_AUDIO_QUALITY] ?: DownloadAudioQuality.HIGH.name)
        } catch (_: Exception) {
            DownloadAudioQuality.HIGH
        }

        val mobileQuality = try {
            AudioQuality.valueOf(prefs[KEY_MOBILE_STREAMING_QUALITY] ?: AudioQuality.DATA_SAVER.name)
        } catch (_: Exception) {
            AudioQuality.DATA_SAVER
        }

        val wifiQuality = try {
            AudioQuality.valueOf(prefs[KEY_WIFI_STREAMING_QUALITY] ?: AudioQuality.HIGH.name)
        } catch (_: Exception) {
            AudioQuality.HIGH
        }

        val notifStyle = try {
            NotificationControlsStyle.valueOf(prefs[KEY_NOTIFICATION_STYLE] ?: NotificationControlsStyle.FULL.name)
        } catch (_: Exception) {
            NotificationControlsStyle.FULL
        }

        AppSettings(
            themeMode = themeMode,
            fontSizeScale = fontScale,
            highContrastMode = prefs[KEY_HIGH_CONTRAST] ?: false,
            defaultAudioQuality = defaultAudio,
            defaultVideoQuality = defaultVideo,
            gaplessPlayback = prefs[KEY_GAPLESS_PLAYBACK] ?: true,
            crossfadeDurationSeconds = prefs[KEY_CROSSFADE_SECONDS] ?: 0,
            playbackSpeed = prefs[KEY_PLAYBACK_SPEED] ?: 1.0f,
            audioNormalization = prefs[KEY_AUDIO_NORMALIZATION] ?: false,
            autoplaySimilar = prefs[KEY_AUTOPLAY_SIMILAR] ?: true,
            resumePlaybackOnOpen = prefs[KEY_RESUME_PLAYBACK] ?: true,
            skipSilence = prefs[KEY_SKIP_SILENCE] ?: false,
            downloadAudioQuality = downloadQuality,
            downloadOnlyOnWifi = prefs[KEY_DOWNLOAD_ONLY_ON_WIFI] ?: true,
            mobileStreamingQuality = mobileQuality,
            wifiStreamingQuality = wifiQuality,
            notificationControlsStyle = notifStyle,
            personalizedRecommendations = prefs[KEY_PERSONALIZED_RECOMMENDATIONS] ?: true
        )
    }

    val themeMode: Flow<ThemeMode> = settings.map { it.themeMode }
    val fontSizeScale: Flow<FontSizeScale> = settings.map { it.fontSizeScale }
    val highContrastMode: Flow<Boolean> = settings.map { it.highContrastMode }
    val defaultVideoQuality: Flow<VideoQuality> = settings.map { it.defaultVideoQuality }
    val defaultAudioQuality: Flow<AudioQuality> = settings.map { it.defaultAudioQuality }
    val downloadOnlyOnWifi: Flow<Boolean> = settings.map { it.downloadOnlyOnWifi }
    val personalizedRecommendations: Flow<Boolean> = settings.map { it.personalizedRecommendations }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setFontSizeScale(scale: FontSizeScale) {
        dataStore.edit { it[KEY_FONT_SIZE_SCALE] = scale.name }
    }

    suspend fun setHighContrastMode(enabled: Boolean) {
        dataStore.edit { it[KEY_HIGH_CONTRAST] = enabled }
    }

    suspend fun setDefaultAudioQuality(quality: AudioQuality) {
        dataStore.edit { it[KEY_DEFAULT_AUDIO_QUALITY] = quality.name }
    }

    suspend fun setDefaultVideoQuality(quality: VideoQuality) {
        dataStore.edit { it[KEY_DEFAULT_VIDEO_QUALITY] = quality.name }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        dataStore.edit { it[KEY_GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setCrossfadeDurationSeconds(seconds: Int) {
        dataStore.edit { it[KEY_CROSSFADE_SECONDS] = seconds.coerceIn(0, 12) }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { it[KEY_PLAYBACK_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setAudioNormalization(enabled: Boolean) {
        dataStore.edit { it[KEY_AUDIO_NORMALIZATION] = enabled }
    }

    suspend fun setAutoplaySimilar(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTOPLAY_SIMILAR] = enabled }
    }

    suspend fun setResumePlaybackOnOpen(enabled: Boolean) {
        dataStore.edit { it[KEY_RESUME_PLAYBACK] = enabled }
    }

    suspend fun setSkipSilence(enabled: Boolean) {
        dataStore.edit { it[KEY_SKIP_SILENCE] = enabled }
    }

    suspend fun setDownloadAudioQuality(quality: DownloadAudioQuality) {
        dataStore.edit { it[KEY_DOWNLOAD_AUDIO_QUALITY] = quality.name }
    }

    suspend fun setDownloadOnlyOnWifi(enabled: Boolean) {
        dataStore.edit { it[KEY_DOWNLOAD_ONLY_ON_WIFI] = enabled }
    }

    suspend fun setMobileStreamingQuality(quality: AudioQuality) {
        dataStore.edit { it[KEY_MOBILE_STREAMING_QUALITY] = quality.name }
    }

    suspend fun setWifiStreamingQuality(quality: AudioQuality) {
        dataStore.edit { it[KEY_WIFI_STREAMING_QUALITY] = quality.name }
    }

    suspend fun setNotificationControlsStyle(style: NotificationControlsStyle) {
        dataStore.edit { it[KEY_NOTIFICATION_STYLE] = style.name }
    }

    suspend fun setPersonalizedRecommendations(enabled: Boolean) {
        dataStore.edit { it[KEY_PERSONALIZED_RECOMMENDATIONS] = enabled }
    }

    suspend fun resetAllToDefaults() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
