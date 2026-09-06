package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.data.preferences.AudioQuality
import com.example.data.preferences.DownloadAudioQuality
import com.example.data.preferences.FontSizeScale
import com.example.data.preferences.NotificationControlsStyle
import com.example.data.preferences.ThemeMode
import com.example.playback.VideoQuality
import com.example.ui.components.NeumorphicInsetSurface
import com.example.ui.components.NeumorphicSurface
import com.example.ui.theme.NeumorphicTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog States
    var showClearDownloadsDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearSearchDialog by remember { mutableStateOf(false) }
    var showResetDefaultsDialog by remember { mutableStateOf(false) }

    // Dropdown/Selection Dialog States
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showVideoQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showMobileQualityDialog by remember { mutableStateOf(false) }
    var showWifiQualityDialog by remember { mutableStateOf(false) }

    // Playlist Export Launcher
    val exportPlaylistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportPlaylistsToUri(context, uri)
        }
    }

    // User feedback listener
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeumorphicTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TOP BAR
            SettingsTopBar(
                onNavigateBack = onNavigateBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ==========================================
                // 2. APPEARANCE SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "APPEARANCE")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // Theme Selection Segmented Control
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ColorLens,
                                    contentDescription = null,
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "App Theme",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeumorphicTheme.colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            NeumorphicSegmentedControl(
                                options = listOf("System", "Light", "Dark"),
                                selectedIndex = when (settings.themeMode) {
                                    ThemeMode.SYSTEM -> 0
                                    ThemeMode.LIGHT -> 1
                                    ThemeMode.DARK -> 2
                                },
                                onOptionSelected = { index ->
                                    val newMode = when (index) {
                                        0 -> ThemeMode.SYSTEM
                                        1 -> ThemeMode.LIGHT
                                        else -> ThemeMode.DARK
                                    }
                                    viewModel.setThemeMode(newMode)
                                }
                            )
                        }

                        SettingsDivider()

                        // Font Size Scale Control
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FontDownload,
                                    contentDescription = null,
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Font Size",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeumorphicTheme.colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            NeumorphicSegmentedControl(
                                options = listOf("Small", "Default", "Large"),
                                selectedIndex = when (settings.fontSizeScale) {
                                    FontSizeScale.SMALL -> 0
                                    FontSizeScale.DEFAULT -> 1
                                    FontSizeScale.LARGE -> 2
                                },
                                onOptionSelected = { index ->
                                    val scale = when (index) {
                                        0 -> FontSizeScale.SMALL
                                        1 -> FontSizeScale.DEFAULT
                                        else -> FontSizeScale.LARGE
                                    }
                                    viewModel.setFontSizeScale(scale)
                                }
                            )
                        }

                        SettingsDivider()

                        // High Contrast Mode Toggle
                        SettingsToggleRow(
                            icon = Icons.Default.Tune,
                            title = "High-contrast mode",
                            subtitle = "Enhance neumorphic shadow and text contrast",
                            checked = settings.highContrastMode,
                            onCheckedChange = { viewModel.setHighContrastMode(it) }
                        )
                    }
                }

                // ==========================================
                // 3. PLAYBACK SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "PLAYBACK")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // Default Audio Quality
                        SettingsClickableOptionRow(
                            icon = Icons.Default.Audiotrack,
                            title = "Default audio quality",
                            value = settings.defaultAudioQuality.label,
                            onClick = { showAudioQualityDialog = true }
                        )

                        SettingsDivider()

                        // Default Video Quality (synchronized with Now Playing selector)
                        SettingsClickableOptionRow(
                            icon = Icons.Default.Videocam,
                            title = "Default video quality",
                            value = when (settings.defaultVideoQuality) {
                                VideoQuality.AUTO -> "Auto"
                                VideoQuality.Q1080P -> "1080p (FHD)"
                                VideoQuality.Q720P -> "720p (HD)"
                                VideoQuality.Q480P -> "480p (SD)"
                            },
                            onClick = { showVideoQualityDialog = true }
                        )

                        SettingsDivider()

                        // Gapless Playback
                        SettingsToggleRow(
                            icon = Icons.Default.GraphicEq,
                            title = "Gapless playback",
                            subtitle = "Continuous transition without silent pauses",
                            checked = settings.gaplessPlayback,
                            onCheckedChange = { viewModel.setGaplessPlayback(it) }
                        )

                        SettingsDivider()

                        // Crossfade Duration Slider
                        SettingsSliderRow(
                            icon = Icons.Default.Tune,
                            title = "Crossfade duration",
                            valueText = if (settings.crossfadeDurationSeconds == 0) "Off" else "${settings.crossfadeDurationSeconds}s",
                            value = settings.crossfadeDurationSeconds.toFloat(),
                            valueRange = 0f..12f,
                            steps = 11,
                            onValueChange = { viewModel.setCrossfadeDurationSeconds(it.roundToInt()) }
                        )

                        SettingsDivider()

                        // Default Playback Speed Slider
                        SettingsSliderRow(
                            icon = Icons.Default.Speed,
                            title = "Default playback speed",
                            valueText = "${String.format("%.2f", settings.playbackSpeed)}x",
                            value = settings.playbackSpeed,
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            onValueChange = { viewModel.setPlaybackSpeed(it) }
                        )

                        SettingsDivider()

                        // Audio Normalization Toggle
                        // TODO: Connect to loudness normalization DSP filter in ExoPlayer
                        SettingsToggleRow(
                            icon = Icons.Default.VolumeUp,
                            title = "Audio normalization",
                            subtitle = "Equalize volume levels across different songs",
                            checked = settings.audioNormalization,
                            onCheckedChange = { viewModel.setAudioNormalization(it) }
                        )

                        SettingsDivider()

                        // Auto-play Similar Songs
                        SettingsToggleRow(
                            icon = Icons.Default.PlayCircle,
                            title = "Auto-play similar songs",
                            subtitle = "Keep listening when the current queue ends",
                            checked = settings.autoplaySimilar,
                            onCheckedChange = { viewModel.setAutoplaySimilar(it) }
                        )

                        SettingsDivider()

                        // Resume Playback on Open
                        SettingsToggleRow(
                            icon = Icons.Default.Restore,
                            title = "Resume playback on app open",
                            subtitle = "Restore your previous track and position",
                            checked = settings.resumePlaybackOnOpen,
                            onCheckedChange = { viewModel.setResumePlaybackOnOpen(it) }
                        )

                        SettingsDivider()

                        // Skip Silence Toggle
                        // TODO: Hook to SilenceDetectionAudioProcessor in ExoPlayer
                        SettingsToggleRow(
                            icon = Icons.Default.MusicNote,
                            title = "Skip silence",
                            subtitle = "Automatically skip silent intros and outros",
                            checked = settings.skipSilence,
                            onCheckedChange = { viewModel.setSkipSilence(it) }
                        )
                    }
                }

                // ==========================================
                // 4. DOWNLOADS SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "DOWNLOADS")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // Download Audio Quality
                        SettingsClickableOptionRow(
                            icon = Icons.Default.Download,
                            title = "Download audio quality",
                            value = settings.downloadAudioQuality.label,
                            onClick = { showDownloadQualityDialog = true }
                        )

                        SettingsDivider()

                        // Download only on Wi-Fi
                        SettingsToggleRow(
                            icon = Icons.Default.Wifi,
                            title = "Download only on Wi-Fi",
                            subtitle = "Require unmetered network for offline downloads",
                            checked = settings.downloadOnlyOnWifi,
                            onCheckedChange = { viewModel.setDownloadOnlyOnWifi(it) }
                        )

                        SettingsDivider()

                        // Storage Used by Downloads + Clear Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Storage used",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = viewModel.formatStorageSize(totalStorageBytes),
                                        fontSize = 12.sp,
                                        color = NeumorphicTheme.colors.textSecondary
                                    )
                                }
                            }

                            NeumorphicSurface(
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("clear_all_downloads_button"),
                                cornerRadius = 18.dp,
                                elevation = 3.dp,
                                onClick = { showClearDownloadsDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear Downloads",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Clear All",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935)
                                    )
                                }
                            }
                        }

                        SettingsDivider()

                        // Download Location Info Row (App-private storage path)
                        // Note: Placeholder for future external/SD card storage selection
                        SettingsInfoRow(
                            icon = Icons.Default.Info,
                            title = "Download location",
                            subtitle = "${context.filesDir.absolutePath}/downloads (Internal App Storage)"
                        )
                    }
                }

                // ==========================================
                // 5. DATA USAGE SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "DATA USAGE")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // Mobile Data Streaming Quality
                        SettingsClickableOptionRow(
                            icon = Icons.Default.Tune,
                            title = "Mobile data streaming quality",
                            value = settings.mobileStreamingQuality.label,
                            onClick = { showMobileQualityDialog = true }
                        )

                        SettingsDivider()

                        // Wi-Fi Streaming Quality
                        SettingsClickableOptionRow(
                            icon = Icons.Default.Wifi,
                            title = "Wi-Fi streaming quality",
                            value = settings.wifiStreamingQuality.label,
                            onClick = { showWifiQualityDialog = true }
                        )
                    }
                }

                // ==========================================
                // 6. NOTIFICATIONS SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "NOTIFICATIONS")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // Note: Foreground service requires an active notification for playback.
                        // This toggle controls full vs minimal controls style on lockscreen.
                        SettingsToggleRow(
                            icon = Icons.Default.Notifications,
                            title = "Full lockscreen playback controls",
                            subtitle = "Display extended controls and like button in notification",
                            checked = settings.notificationControlsStyle == NotificationControlsStyle.FULL,
                            onCheckedChange = { isFull ->
                                viewModel.setNotificationControlsStyle(
                                    if (isFull) NotificationControlsStyle.FULL else NotificationControlsStyle.MINIMAL
                                )
                            }
                        )
                    }
                }

                // ==========================================
                // 7. LIBRARY & HISTORY SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "LIBRARY & HISTORY")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // Clear Recently Played History
                        SettingsActionRow(
                            icon = Icons.Default.History,
                            title = "Clear recently played history",
                            subtitle = "Erase your recent track listening history",
                            onClick = { showClearHistoryDialog = true }
                        )

                        SettingsDivider()

                        // Clear Search History
                        SettingsActionRow(
                            icon = Icons.Default.Search,
                            title = "Clear search history",
                            subtitle = "Erase saved search queries and suggestions",
                            onClick = { showClearSearchDialog = true }
                        )
                    }
                }

                // ==========================================
                // 8. PRIVACY SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "PRIVACY")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // Personalized Recommendations Toggle
                        SettingsToggleRow(
                            icon = Icons.Default.Security,
                            title = "Personalized recommendations",
                            subtitle = "When off, Home uses Indian curated charts without personal history",
                            checked = settings.personalizedRecommendations,
                            onCheckedChange = { viewModel.setPersonalizedRecommendations(it) }
                        )

                        SettingsDivider()

                        // Reset All Preferences to Default
                        SettingsActionRow(
                            icon = Icons.Default.Restore,
                            title = "Reset all preferences to default",
                            subtitle = "Revert all settings and toggles to factory state",
                            titleColor = Color(0xFFE53935),
                            iconTint = Color(0xFFE53935),
                            onClick = { showResetDefaultsDialog = true }
                        )
                    }
                }

                // ==========================================
                // 9. BACKUP SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "BACKUP")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // Export Playlists
                        SettingsActionRow(
                            icon = Icons.Default.Backup,
                            title = "Export my playlists",
                            subtitle = "Save custom playlists and song metadata to a JSON file",
                            onClick = {
                                exportPlaylistLauncher.launch("swar_playlists_backup.json")
                            }
                        )
                    }
                }

                // ==========================================
                // 10. LANGUAGE SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "LANGUAGE")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = NeumorphicTheme.colors.textSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Language",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTheme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "English (India)",
                                        fontSize = 12.sp,
                                        color = NeumorphicTheme.colors.textSecondary.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeumorphicTheme.colors.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Coming soon",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeumorphicTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 11. WIDGETS & SHORTCUTS SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "WIDGETS & SHORTCUTS")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = NeumorphicTheme.colors.textSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Home screen widgets",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTheme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Quick playback controller & playlist tiles",
                                        fontSize = 12.sp,
                                        color = NeumorphicTheme.colors.textSecondary.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeumorphicTheme.colors.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Coming soon",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeumorphicTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 12. ABOUT SECTION
                // ==========================================
                item {
                    SettingsSectionHeader(title = "ABOUT")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsGroupCard {
                        // App Version Row
                        SettingsInfoRow(
                            icon = Icons.Default.Info,
                            title = "App version",
                            subtitle = "SWAR Music v${BuildConfig.VERSION_NAME} (Build 1)"
                        )

                        SettingsDivider()

                        // Check for updates
                        // TODO: Connect to GitHub Releases API tag inspector
                        SettingsActionRow(
                            icon = Icons.Default.SystemUpdate,
                            title = "Check for updates",
                            subtitle = "Ensure you are using the latest release",
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("You're on the latest version of SWAR Music (v${BuildConfig.VERSION_NAME})")
                                }
                            }
                        )

                        SettingsDivider()

                        // Send feedback / report bug
                        SettingsActionRow(
                            icon = Icons.Default.Email,
                            title = "Send feedback / report a bug",
                            subtitle = "Share suggestions or issue reports via email",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:swanmusicplayer@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "SWAR Music Feedback (v${BuildConfig.VERSION_NAME})")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Email client not available on device")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Clear All Downloads Dialog
    if (showClearDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsDialog = false },
            title = {
                Text(
                    text = "Clear All Downloads",
                    color = NeumorphicTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will delete all offline audio files (${viewModel.formatStorageSize(totalStorageBytes)}) from your storage.",
                    fontSize = 13.sp,
                    color = NeumorphicTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDownloadsDialog = false
                        viewModel.clearAllDownloads()
                    }
                ) {
                    Text("Delete All", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDownloadsDialog = false }) {
                    Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
                }
            },
            containerColor = NeumorphicTheme.colors.background
        )
    }

    // 3. Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Clear Listening History",
                    color = NeumorphicTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will erase your recently played tracks across this device and cloud sync.",
                    fontSize = 13.sp,
                    color = NeumorphicTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearRecentlyPlayedHistory()
                    }
                ) {
                    Text("Clear", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
                }
            },
            containerColor = NeumorphicTheme.colors.background
        )
    }

    // 4. Clear Search History Dialog
    if (showClearSearchDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchDialog = false },
            title = {
                Text(
                    text = "Clear Search History",
                    color = NeumorphicTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear your recent search history?",
                    fontSize = 13.sp,
                    color = NeumorphicTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearSearchDialog = false
                        viewModel.clearSearchHistory()
                    }
                ) {
                    Text("Clear", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchDialog = false }) {
                    Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
                }
            },
            containerColor = NeumorphicTheme.colors.background
        )
    }

    // 5. Reset Defaults Dialog
    if (showResetDefaultsDialog) {
        AlertDialog(
            onDismissRequest = { showResetDefaultsDialog = false },
            title = {
                Text(
                    text = "Reset All Settings",
                    color = NeumorphicTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will restore all playback, download, appearance, and privacy settings to their default values.",
                    fontSize = 13.sp,
                    color = NeumorphicTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDefaultsDialog = false
                        viewModel.resetAllPreferences()
                    }
                ) {
                    Text("Reset", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDefaultsDialog = false }) {
                    Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
                }
            },
            containerColor = NeumorphicTheme.colors.background
        )
    }

    // 6. Audio Quality Selection Dialog
    if (showAudioQualityDialog) {
        SingleChoiceDialog(
            title = "Default Audio Quality",
            options = AudioQuality.entries.map { it.label },
            selectedIndex = AudioQuality.entries.indexOf(settings.defaultAudioQuality),
            onSelect = { index ->
                viewModel.setDefaultAudioQuality(AudioQuality.entries[index])
                showAudioQualityDialog = false
            },
            onDismiss = { showAudioQualityDialog = false }
        )
    }

    // 7. Video Quality Selection Dialog
    if (showVideoQualityDialog) {
        val videoOptions = listOf(
            VideoQuality.AUTO to "Auto",
            VideoQuality.Q1080P to "1080p (FHD)",
            VideoQuality.Q720P to "720p (HD)",
            VideoQuality.Q480P to "480p (SD)"
        )
        SingleChoiceDialog(
            title = "Default Video Quality",
            options = videoOptions.map { it.second },
            selectedIndex = videoOptions.indexOfFirst { it.first == settings.defaultVideoQuality }.coerceAtLeast(0),
            onSelect = { index ->
                viewModel.setDefaultVideoQuality(videoOptions[index].first)
                showVideoQualityDialog = false
            },
            onDismiss = { showVideoQualityDialog = false }
        )
    }

    // 8. Download Audio Quality Selection Dialog
    if (showDownloadQualityDialog) {
        SingleChoiceDialog(
            title = "Download Audio Quality",
            options = DownloadAudioQuality.entries.map { it.label },
            selectedIndex = DownloadAudioQuality.entries.indexOf(settings.downloadAudioQuality),
            onSelect = { index ->
                viewModel.setDownloadAudioQuality(DownloadAudioQuality.entries[index])
                showDownloadQualityDialog = false
            },
            onDismiss = { showDownloadQualityDialog = false }
        )
    }

    // 9. Mobile Data Quality Selection Dialog
    if (showMobileQualityDialog) {
        SingleChoiceDialog(
            title = "Mobile Data Streaming Quality",
            options = AudioQuality.entries.map { it.label },
            selectedIndex = AudioQuality.entries.indexOf(settings.mobileStreamingQuality),
            onSelect = { index ->
                viewModel.setMobileStreamingQuality(AudioQuality.entries[index])
                showMobileQualityDialog = false
            },
            onDismiss = { showMobileQualityDialog = false }
        )
    }

    // 10. Wi-Fi Quality Selection Dialog
    if (showWifiQualityDialog) {
        SingleChoiceDialog(
            title = "Wi-Fi Streaming Quality",
            options = AudioQuality.entries.map { it.label },
            selectedIndex = AudioQuality.entries.indexOf(settings.wifiStreamingQuality),
            onSelect = { index ->
                viewModel.setWifiStreamingQuality(AudioQuality.entries[index])
                showWifiQualityDialog = false
            },
            onDismiss = { showWifiQualityDialog = false }
        )
    }
}

// =========================================================================
// NEUMORPHIC SETTINGS BUILDING BLOCKS
// =========================================================================

@Composable
fun SettingsTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeumorphicSurface(
            modifier = Modifier
                .size(42.dp)
                .testTag("settings_back_button"),
            isCircle = true,
            elevation = 4.dp,
            onClick = onNavigateBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = NeumorphicTheme.colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NeumorphicTheme.colors.textPrimary
            )
            Text(
                text = "Preferences, playback & account",
                fontSize = 12.sp,
                color = NeumorphicTheme.colors.textSecondary
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = NeumorphicTheme.colors.accent,
        modifier = modifier.padding(start = 6.dp, top = 4.dp)
    )
}

@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    NeumorphicSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        elevation = 5.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = NeumorphicTheme.colors.surfaceVariant.copy(alpha = 0.4f)
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphicTheme.colors.textPrimary
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = NeumorphicTheme.colors.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        NeumorphicSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsClickableOptionRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeumorphicTheme.colors.accent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeumorphicTheme.colors.textPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NeumorphicTheme.colors.accent
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = NeumorphicTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = NeumorphicTheme.colors.textPrimary,
    iconTint: Color = NeumorphicTheme.colors.accent
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = NeumorphicTheme.colors.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = NeumorphicTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeumorphicTheme.colors.accent,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeumorphicTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = NeumorphicTheme.colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun SettingsSliderRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeumorphicTheme.colors.accent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphicTheme.colors.textPrimary
                )
            }
            Text(
                text = valueText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeumorphicTheme.colors.accent
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = NeumorphicTheme.colors.accent,
                activeTrackColor = NeumorphicTheme.colors.accent,
                inactiveTrackColor = NeumorphicTheme.colors.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Clean Neumorphic Segmented Button Control
 */
@Composable
fun NeumorphicSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NeumorphicInsetSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        depth = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                if (isSelected) {
                    NeumorphicSurface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        cornerRadius = 10.dp,
                        elevation = 3.dp,
                        onClick = { onOptionSelected(index) }
                    ) {
                        Text(
                            text = option,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTheme.colors.accent
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onOptionSelected(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeumorphicTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Neumorphic Switch (Toggle)
 */
@Composable
fun NeumorphicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = tween(durationMillis = 200),
        label = "thumb_offset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) NeumorphicTheme.colors.accent.copy(alpha = 0.25f) else NeumorphicTheme.colors.background,
        animationSpec = tween(durationMillis = 200),
        label = "track_color"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "thumb_color"
    )

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 30.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        NeumorphicInsetSurface(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 15.dp,
            depth = 2.dp,
            backgroundColor = trackColor
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 3.dp)
            ) {
                NeumorphicSurface(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = thumbOffset),
                    isCircle = true,
                    elevation = 3.dp,
                    backgroundColor = NeumorphicTheme.colors.background
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(thumbColor)
                    )
                }
            }
        }
    }
}

/**
 * Single Choice Selection Dialog
 */
@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = NeumorphicTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NeumorphicTheme.colors.accent,
                                unselectedColor = NeumorphicTheme.colors.textSecondary
                            )
                        )
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selectedIndex) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
            }
        },
        containerColor = NeumorphicTheme.colors.background
    )
}
