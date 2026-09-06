package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Theme Tokens
val LightBackground = Color(0xFFE8ECF1)
val LightRaisedShadowDark = Color(0xFFC3C9D2)
val LightRaisedShadowLight = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF2E3440)
val LightTextSecondary = Color(0xFF838B99)
val LightAccent = Color(0xFF33C29B)

// Light High-Contrast Tokens
val LightHighContrastBackground = Color(0xFFEFF2F6)
val LightHighContrastShadowDark = Color(0xFFA6AFBC)
val LightHighContrastShadowLight = Color(0xFFFFFFFF)
val LightHighContrastTextPrimary = Color(0xFF11151C)
val LightHighContrastTextSecondary = Color(0xFF555E6D)
val LightHighContrastAccent = Color(0xFF0D9488)

// Dark Theme Tokens
val DarkBackground = Color(0xFF2B2E36)
val DarkRaisedShadowLight = Color(0xFF363A44)
val DarkRaisedShadowDark = Color(0xFF1D1F25)
val DarkTextPrimary = Color(0xFFF2F3F5)
val DarkTextSecondary = Color(0xFF9AA0A6)
val DarkAccent = Color(0xFF33C29B)

// Dark High-Contrast Tokens
val DarkHighContrastBackground = Color(0xFF1C1E24)
val DarkHighContrastShadowLight = Color(0xFF3E4350)
val DarkHighContrastShadowDark = Color(0xFF0C0D10)
val DarkHighContrastTextPrimary = Color(0xFFFFFFFF)
val DarkHighContrastTextSecondary = Color(0xFFB5BAC4)
val DarkHighContrastAccent = Color(0xFF34D399)

@Immutable
data class NeumorphicColors(
    val background: Color,
    val raisedShadowDark: Color,
    val raisedShadowLight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val isDark: Boolean,
    val isHighContrast: Boolean = false
) {
    val surfaceVariant: Color
        get() = if (isDark) raisedShadowLight else raisedShadowDark
}

val LightNeumorphicColors = NeumorphicColors(
    background = LightBackground,
    raisedShadowDark = LightRaisedShadowDark,
    raisedShadowLight = LightRaisedShadowLight,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    accent = LightAccent,
    isDark = false,
    isHighContrast = false
)

val LightHighContrastNeumorphicColors = NeumorphicColors(
    background = LightHighContrastBackground,
    raisedShadowDark = LightHighContrastShadowDark,
    raisedShadowLight = LightHighContrastShadowLight,
    textPrimary = LightHighContrastTextPrimary,
    textSecondary = LightHighContrastTextSecondary,
    accent = LightHighContrastAccent,
    isDark = false,
    isHighContrast = true
)

val DarkNeumorphicColors = NeumorphicColors(
    background = DarkBackground,
    raisedShadowDark = DarkRaisedShadowDark,
    raisedShadowLight = DarkRaisedShadowLight,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    accent = DarkAccent,
    isDark = true,
    isHighContrast = false
)

val DarkHighContrastNeumorphicColors = NeumorphicColors(
    background = DarkHighContrastBackground,
    raisedShadowDark = DarkHighContrastShadowDark,
    raisedShadowLight = DarkHighContrastShadowLight,
    textPrimary = DarkHighContrastTextPrimary,
    textSecondary = DarkHighContrastTextSecondary,
    accent = DarkHighContrastAccent,
    isDark = true,
    isHighContrast = true
)

val LocalNeumorphicColors = compositionLocalOf { LightNeumorphicColors }
