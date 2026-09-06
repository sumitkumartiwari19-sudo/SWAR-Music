package com.example.ui.screens.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.ui.ads.AdBannerView
import com.example.ui.ads.AdUnit
import com.example.ui.ads.INLINE_AD_INTERVAL
import com.example.ui.ads.ListItemWithAd
import com.example.ui.ads.interleaveWithAds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.CuratedTracks
import com.example.data.model.Track
import com.example.data.preferences.ThemeMode
import com.example.ui.components.NeumorphicEqualizer
import com.example.ui.components.NeumorphicInsetSurface
import com.example.ui.components.NeumorphicSurface
import com.example.ui.playback.PlaybackViewModel
import com.example.ui.theme.NeumorphicTheme
import java.util.Calendar

@Composable
fun HomeScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel,
    playbackViewModel: PlaybackViewModel = viewModel(
        factory = PlaybackViewModel.provideFactory(LocalContext.current)
    ),
    modifier: Modifier = Modifier
) {
    var selectedChipIndex by remember { mutableStateOf(0) }

    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val trendingNow by viewModel.trendingNow.collectAsStateWithLifecycle()
    val newReleases by viewModel.newReleases.collectAsStateWithLifecycle()

    val chips = listOf("All", "Quick Picks", "Trending Hits", "New & Regional", "Classical Ragas", "Lo-Fi Lounge")

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Night vibes"
        }
    }

    // Prepare list of recently played or curated fallbacks
    val recentItemsList: List<Track> = remember(recentlyPlayed) {
        if (recentlyPlayed.isNotEmpty()) {
            recentlyPlayed.map { it.track }
        } else {
            CuratedTracks.allCurated.take(5)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeumorphicTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // 1. TOP BAR: Greeting Text + Theme & Settings Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting,",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeumorphicTheme.colors.textSecondary
                    )
                    Text(
                        text = "SWAR Music",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTheme.colors.textPrimary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Theme Switcher Button (Raised Circle)
                    NeumorphicSurface(
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("theme_toggle_button"),
                        isCircle = true,
                        elevation = 4.dp,
                        onClick = {
                            val nextMode = when (currentThemeMode) {
                                ThemeMode.LIGHT -> ThemeMode.DARK
                                ThemeMode.DARK -> ThemeMode.SYSTEM
                                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                            }
                            onThemeModeChange(nextMode)
                        }
                    ) {
                        val icon = when (currentThemeMode) {
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Toggle Theme",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Raised Settings Button
                    NeumorphicSurface(
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("home_settings_button"),
                        isCircle = true,
                        elevation = 4.dp,
                        onClick = onNavigateToSettings
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NeumorphicTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. INSET (PRESSED-IN) SEARCH BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                NeumorphicInsetSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable(onClick = onNavigateToSearch)
                        .testTag("home_search_bar_inset"),
                    cornerRadius = 26.dp,
                    depth = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search tracks, artists, ragas, albums...",
                            fontSize = 14.sp,
                            color = NeumorphicTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. HORIZONTAL FILTER CHIPS (Raised when inactive, filled with accent when active)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chips.forEachIndexed { index, chipTitle ->
                    val isSelected = selectedChipIndex == index

                    if (isSelected) {
                        // Active chip: Filled with accent color
                        NeumorphicSurface(
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("filter_chip_$index"),
                            cornerRadius = 19.dp,
                            elevation = 4.dp,
                            backgroundColor = NeumorphicTheme.colors.accent,
                            onClick = { selectedChipIndex = index }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chipTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Inactive chip: Raised surface
                        NeumorphicSurface(
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("filter_chip_$index"),
                            cornerRadius = 19.dp,
                            elevation = 4.dp,
                            onClick = { selectedChipIndex = index }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chipTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeumorphicTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val showAll = selectedChipIndex == 0
            val showQuickPicks = showAll || selectedChipIndex == 1
            val showTrending = showAll || selectedChipIndex == 2
            val showNewReleases = showAll || selectedChipIndex == 3
            val showClassical = selectedChipIndex == 4
            val showLofi = selectedChipIndex == 5

            val classicalFiltered = remember(quickPicks, trendingNow, newReleases) {
                (quickPicks + trendingNow + newReleases).filter {
                    it.title.contains("Raga", true) || it.title.contains("Raag", true) ||
                            it.artist.contains("Pandit", true) || it.artist.contains("Sitar", true) ||
                            it.artist.contains("Classical", true)
                }.distinctBy { it.id }
            }

            val lofiFiltered = remember(quickPicks, trendingNow, newReleases) {
                (quickPicks + trendingNow + newReleases).filter {
                    it.title.contains("Lo-Fi", true) || it.title.contains("Lofi", true) ||
                            it.title.contains("Chill", true) || it.title.contains("Acoustic", true)
                }.distinctBy { it.id }
            }

            // 4. HORIZONTAL SECTION: "QUICK PICKS"
            if (showQuickPicks) {
                SectionHeader(
                    title = "Quick Picks",
                    subtitle = "Handpicked for your mood & history",
                    onSeeAll = onNavigateToSearch
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(quickPicks, key = { it.id }) { track ->
                        val isCurrentTrack = playbackState.currentTrack?.id == track.id
                        val isCurrentPlaying = isCurrentTrack && playbackState.isPlaying
                        val isLiked = likedSongs.any { it.track.id == track.id }

                        QuickPickCard(
                            track = track,
                            isPlaying = isCurrentPlaying,
                            isLiked = isLiked,
                            onClick = {
                                playbackViewModel.playTrack(track, quickPicks)
                                viewModel.recordTrackPlayed(track)
                            },
                            onToggleLike = {
                                viewModel.toggleLikedSong(track)
                            }
                        )
                    }

                    // AD UNIT 2 — Inline Native Banner in suggestion row (matches 150dp card width)
                    item(key = "home_quick_picks_native_ad") {
                        AdBannerView(
                            adUnit = AdUnit.NativeBanner1x1(sizeDp = 150),
                            showContainerCard = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // AD UNIT 3 — 300x250 Featured Banner (Prominently placed in always-visible location)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AdBannerView(
                    adUnit = AdUnit.Banner300x250(),
                    showContainerCard = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. HORIZONTAL SECTION: "TRENDING NOW"
            if (showTrending) {
                SectionHeader(
                    title = "Trending Now",
                    subtitle = "Current Indian music trends & charts",
                    onSeeAll = onNavigateToSearch
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(trendingNow, key = { it.id }) { track ->
                        val isCurrentTrack = playbackState.currentTrack?.id == track.id
                        val isCurrentPlaying = isCurrentTrack && playbackState.isPlaying
                        val isLiked = likedSongs.any { it.track.id == track.id }

                        TrendingCard(
                            track = track,
                            isPlaying = isCurrentPlaying,
                            isLiked = isLiked,
                            onClick = {
                                playbackViewModel.playTrack(track, trendingNow)
                                viewModel.recordTrackPlayed(track)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 6. HORIZONTAL SECTION: "NEW & REGIONAL RELEASES"
            if (showNewReleases) {
                SectionHeader(
                    title = "New & Regional Hits",
                    subtitle = "Fresh Bollywood, Punjabi, Telugu & Tamil singles",
                    onSeeAll = onNavigateToSearch
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newReleases, key = { it.id }) { track ->
                        val isCurrentTrack = playbackState.currentTrack?.id == track.id
                        val isCurrentPlaying = isCurrentTrack && playbackState.isPlaying
                        val isLiked = likedSongs.any { it.track.id == track.id }

                        TrendingCard(
                            track = track,
                            isPlaying = isCurrentPlaying,
                            isLiked = isLiked,
                            onClick = {
                                playbackViewModel.playTrack(track, newReleases)
                                viewModel.recordTrackPlayed(track)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 7. FILTERED VIEW FOR CLASSICAL / LOFI CHIPS
            if (showClassical) {
                SectionHeader(
                    title = "Classical Ragas & Fusion",
                    subtitle = "Instrumental sitar, bansuri & sarod melodies",
                    onSeeAll = onNavigateToSearch
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val tracksToShow = if (classicalFiltered.isNotEmpty()) classicalFiltered else CuratedTracks.allCurated
                    items(tracksToShow, key = { it.id }) { track ->
                        val isCurrentTrack = playbackState.currentTrack?.id == track.id
                        val isCurrentPlaying = isCurrentTrack && playbackState.isPlaying
                        val isLiked = likedSongs.any { it.track.id == track.id }

                        QuickPickCard(
                            track = track,
                            isPlaying = isCurrentPlaying,
                            isLiked = isLiked,
                            onClick = {
                                playbackViewModel.playTrack(track, tracksToShow)
                                viewModel.recordTrackPlayed(track)
                            },
                            onToggleLike = {
                                viewModel.toggleLikedSong(track)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            if (showLofi) {
                SectionHeader(
                    title = "Lo-Fi Lounge & Chill",
                    subtitle = "Relaxing Indian acoustic & slowed aesthetics",
                    onSeeAll = onNavigateToSearch
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val tracksToShow = if (lofiFiltered.isNotEmpty()) lofiFiltered else CuratedTracks.allCurated
                    items(tracksToShow, key = { it.id }) { track ->
                        val isCurrentTrack = playbackState.currentTrack?.id == track.id
                        val isCurrentPlaying = isCurrentTrack && playbackState.isPlaying
                        val isLiked = likedSongs.any { it.track.id == track.id }

                        QuickPickCard(
                            track = track,
                            isPlaying = isCurrentPlaying,
                            isLiked = isLiked,
                            onClick = {
                                playbackViewModel.playTrack(track, tracksToShow)
                                viewModel.recordTrackPlayed(track)
                            },
                            onToggleLike = {
                                viewModel.toggleLikedSong(track)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 8. VERTICAL SECTION: "RECENTLY PLAYED"
            SectionHeader(
                title = "Recently Played",
                subtitle = "Your listening trail",
                onSeeAll = null
            )

            Spacer(modifier = Modifier.height(12.dp))

            val displayRecent = remember(recentItemsList) {
                recentItemsList.interleaveWithAds(INLINE_AD_INTERVAL)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                displayRecent.forEach { displayItem ->
                    when (displayItem) {
                        is ListItemWithAd.Content -> {
                            val track = displayItem.item
                            val isCurrentTrack = playbackState.currentTrack?.id == track.id
                            val isCurrentPlaying = isCurrentTrack && playbackState.isPlaying
                            val isLiked = likedSongs.any { it.track.id == track.id }

                            RecentlyPlayedRow(
                                track = track,
                                isPlaying = isCurrentPlaying,
                                isCurrentTrack = isCurrentTrack,
                                isLiked = isLiked,
                                formatDuration = { playbackViewModel.formatDuration(it * 1000L) },
                                onClick = {
                                    if (isCurrentTrack) {
                                        playbackViewModel.togglePlayPause()
                                    } else {
                                        playbackViewModel.playTrack(track, recentItemsList)
                                        viewModel.recordTrackPlayed(track)
                                    }
                                },
                                onToggleLike = {
                                    viewModel.toggleLikedSong(track)
                                }
                            )
                        }
                        is ListItemWithAd.InlineAd -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AdBannerView(
                                    adUnit = displayItem.adUnit,
                                    showContainerCard = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NeumorphicTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = NeumorphicTheme.colors.textSecondary
            )
        }
        if (onSeeAll != null) {
            Text(
                text = "See all",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeumorphicTheme.colors.accent,
                modifier = Modifier.clickable(onClick = onSeeAll)
            )
        }
    }
}

@Composable
private fun QuickPickCard(
    track: Track,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit
) {
    NeumorphicSurface(
        modifier = Modifier
            .width(150.dp)
            .testTag("quick_pick_${track.id}"),
        cornerRadius = 18.dp,
        elevation = if (isPlaying) 2.dp else 6.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Album Art Area (Raised circular or rounded square)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeumorphicTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (track.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeumorphicTheme.colors.accent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Equalizer overlay
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NeumorphicEqualizer(
                            isPlaying = true,
                            barColor = Color.White,
                            barWidth = 4.dp,
                            maxHeight = 22.dp,
                            spacing = 3.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = track.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                fontSize = 11.sp,
                color = NeumorphicTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrendingCard(
    track: Track,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit
) {
    NeumorphicSurface(
        modifier = Modifier
            .width(200.dp)
            .testTag("trending_card_${track.id}"),
        cornerRadius = 18.dp,
        elevation = if (isPlaying) 2.dp else 6.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeumorphicTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (track.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeumorphicTheme.colors.accent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Top right tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HOT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252)
                    )
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NeumorphicEqualizer(
                            isPlaying = true,
                            barColor = Color.White,
                            barWidth = 4.dp,
                            maxHeight = 22.dp,
                            spacing = 3.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = track.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                fontSize = 11.sp,
                color = NeumorphicTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentlyPlayedRow(
    track: Track,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    isLiked: Boolean,
    formatDuration: (Long) -> String,
    onClick: () -> Unit,
    onToggleLike: () -> Unit
) {
    NeumorphicSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_row_${track.id}"),
        cornerRadius = 16.dp,
        elevation = if (isCurrentTrack) 2.dp else 4.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Track Art Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeumorphicTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (track.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeumorphicTheme.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Overlay play state or animated equalizer
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NeumorphicEqualizer(
                            isPlaying = true,
                            barColor = Color.White,
                            barWidth = 3.dp,
                            maxHeight = 16.dp,
                            spacing = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCurrentTrack) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isPlaying) {
                        Spacer(modifier = Modifier.width(6.dp))
                        NeumorphicEqualizer(
                            isPlaying = true,
                            barColor = NeumorphicTheme.colors.accent,
                            barWidth = 2.5.dp,
                            maxHeight = 14.dp,
                            spacing = 1.5.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${track.artist} • ${formatDuration(track.durationSeconds.toLong())}",
                    fontSize = 11.sp,
                    color = NeumorphicTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Like Heart Button
            NeumorphicSurface(
                modifier = Modifier.size(36.dp),
                isCircle = true,
                elevation = 2.dp,
                onClick = onToggleLike
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play/Pause Action
            NeumorphicSurface(
                modifier = Modifier.size(38.dp),
                isCircle = true,
                elevation = 3.dp,
                onClick = onClick
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NeumorphicTheme.colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
