package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.data.preferences.FontSizeScale
import com.example.data.preferences.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightBackground,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightBackground,
    onSurface = LightTextPrimary,
    surfaceVariant = LightRaisedShadowDark,
    onSurfaceVariant = LightTextSecondary,
    outline = LightRaisedShadowDark
)

private val LightHighContrastColorScheme = lightColorScheme(
    primary = LightHighContrastAccent,
    onPrimary = LightHighContrastBackground,
    background = LightHighContrastBackground,
    onBackground = LightHighContrastTextPrimary,
    surface = LightHighContrastBackground,
    onSurface = LightHighContrastTextPrimary,
    surfaceVariant = LightHighContrastShadowDark,
    onSurfaceVariant = LightHighContrastTextSecondary,
    outline = LightHighContrastShadowDark
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkBackground,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkRaisedShadowLight,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkRaisedShadowLight
)

private val DarkHighContrastColorScheme = darkColorScheme(
    primary = DarkHighContrastAccent,
    onPrimary = DarkHighContrastBackground,
    background = DarkHighContrastBackground,
    onBackground = DarkHighContrastTextPrimary,
    surface = DarkHighContrastBackground,
    onSurface = DarkHighContrastTextPrimary,
    surfaceVariant = DarkHighContrastShadowLight,
    onSurfaceVariant = DarkHighContrastTextSecondary,
    outline = DarkHighContrastShadowLight
)

object NeumorphicTheme {
    val colors: NeumorphicColors
        @Composable
        get() = LocalNeumorphicColors.current
}

@Composable
fun SwarMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    highContrast: Boolean = false,
    fontSizeScale: FontSizeScale = FontSizeScale.DEFAULT,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val neumorphicColors = when {
        isDark && highContrast -> DarkHighContrastNeumorphicColors
        isDark && !highContrast -> DarkNeumorphicColors
        !isDark && highContrast -> LightHighContrastNeumorphicColors
        else -> LightNeumorphicColors
    }

    val materialColorScheme = when {
        isDark && highContrast -> DarkHighContrastColorScheme
        isDark && !highContrast -> DarkColorScheme
        !isDark && highContrast -> LightHighContrastColorScheme
        else -> LightColorScheme
    }

    val scaledTypography = getScaledTypography(fontSizeScale.scaleFactor)

    CompositionLocalProvider(LocalNeumorphicColors provides neumorphicColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}
