package com.example.data.preferences

import android.content.Context
import com.example.playback.VideoQuality
import kotlinx.coroutines.flow.Flow

/**
 * Backward compatibility wrapper for ThemePreferences, delegating to SettingsPreferences.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class ThemePreferences(private val context: Context) {
    private val settingsPreferences = SettingsPreferences(context)

    val themeMode: Flow<ThemeMode> = settingsPreferences.themeMode

    suspend fun setThemeMode(mode: ThemeMode) {
        settingsPreferences.setThemeMode(mode)
    }

    val videoQuality: Flow<VideoQuality> = settingsPreferences.defaultVideoQuality

    suspend fun setVideoQuality(quality: VideoQuality) {
        settingsPreferences.setDefaultVideoQuality(quality)
    }
}
