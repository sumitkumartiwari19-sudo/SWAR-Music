package com.example.ui.screens.playlist

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.YoutubeSearchedFor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.ads.AdBannerView
import com.example.ui.ads.AdUnit
import com.example.ui.ads.INLINE_AD_INTERVAL
import com.example.ui.ads.ListItemWithAd
import com.example.ui.ads.canShowFooterAd
import com.example.ui.ads.interleaveWithAds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.local.entity.DownloadStatus
import com.example.data.model.CuratedTracks
import com.example.data.model.CustomPlaylist
import com.example.data.model.ImportedYouTubePlaylist
import com.example.data.model.Track
import com.example.ui.components.NeumorphicEqualizer
import com.example.ui.components.NeumorphicInsetSurface
import com.example.ui.components.NeumorphicSurface
import com.example.ui.playback.PlaybackViewModel
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.theme.NeumorphicTheme
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    isImported: Boolean,
    onNavigateBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(
        factory = PlaybackViewModel.provideFactory(LocalContext.current)
    ),
    modifier: Modifier = Modifier
) {
    val customPlaylists by homeViewModel.customPlaylists.collectAsStateWithLifecycle()
    val importedPlaylists by homeViewModel.importedPlaylists.collectAsStateWithLifecycle()
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val downloadedTracks by homeViewModel.downloadedTracks.collectAsStateWithLifecycle()

    val customPlaylist: CustomPlaylist? = if (!isImported) customPlaylists.find { it.id == playlistId } else null
    val importedPlaylist: ImportedYouTubePlaylist? = if (isImported) importedPlaylists.find { it.id == playlistId } else null

    val title = customPlaylist?.name ?: importedPlaylist?.title ?: "Playlist"
    val author = if (isImported) (importedPlaylist?.author ?: "YouTube") else "You"
    val tracks = customPlaylist?.tracks ?: importedPlaylist?.tracks ?: emptyList()

    var showAddTrackDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    // Smoothly calculate collapse progress from scroll offset
    val collapseFraction by remember {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            if (firstIndex > 1) {
                1f
            } else if (firstIndex == 1) {
                0.6f + (firstOffset.toFloat() / 150f).coerceIn(0f, 0.4f)
            } else {
                (firstOffset.toFloat() / 250f).coerceIn(0f, 0.6f)
            }
        }
    }

    val animatedFraction by animateFloatAsState(
        targetValue = collapseFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "collapse_anim"
    )

    // Add track dialog
    if (showAddTrackDialog) {
        AddTrackToPlaylistDialog(
            existingTracks = tracks,
            onDismiss = { showAddTrackDialog = false },
            onTrackSelected = { newTrack ->
                homeViewModel.addTrackToPlaylist(playlistId, newTrack)
                showAddTrackDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Added \"${newTrack.title}\" to playlist")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Playlist",
                    color = NeumorphicTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"$title\" from your library?",
                    fontSize = 13.sp,
                    color = NeumorphicTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isImported) {
                            homeViewModel.deleteImportedPlaylist(playlistId)
                        } else {
                            homeViewModel.deletePlaylist(playlistId)
                        }
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
                }
            },
            containerColor = NeumorphicTheme.colors.background
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeumorphicTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // 1. UNIFIED SCROLLING CONTENT (Header Items + Tracks)
            val displayPlaylistItems = remember(tracks) {
                tracks.interleaveWithAds(INLINE_AD_INTERVAL)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("playlist_tracks_list"),
                contentPadding = PaddingValues(
                    top = 68.dp, // Space for the pinned top bar
                    bottom = 40.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item 0: Playlist Header Hero Card (Animates out on scroll down)
                item(key = "header_hero_card") {
                    val heroAlpha = (1f - (animatedFraction * 1.3f)).coerceIn(0f, 1f)
                    val heroScale = 1f - (animatedFraction * 0.08f)

                    NeumorphicSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = heroAlpha
                                scaleX = heroScale
                                scaleY = heroScale
                            },
                        cornerRadius = 20.dp,
                        elevation = (6.dp * (1f - animatedFraction)).coerceAtLeast(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Artwork Box
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NeumorphicTheme.colors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                val firstArtwork = tracks.firstOrNull { it.thumbnailUrl.isNotBlank() }?.thumbnailUrl
                                if (firstArtwork != null) {
                                    AsyncImage(
                                        model = firstArtwork,
                                        contentDescription = title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isImported) Icons.Default.YoutubeSearchedFor else Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = NeumorphicTheme.colors.accent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTheme.colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Created by $author",
                                    fontSize = 12.sp,
                                    color = NeumorphicTheme.colors.accent,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${tracks.size} tracks • ${formatTotalDuration(tracks)}",
                                    fontSize = 11.sp,
                                    color = NeumorphicTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Item 1: Action Row: Play All, Shuffle, Download All, Add Track (Animates out on scroll down)
                item(key = "header_action_row") {
                    val actionAlpha = (1f - (animatedFraction * 1.5f)).coerceIn(0f, 1f)
                    val actionScale = 1f - (animatedFraction * 0.08f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = actionAlpha
                                scaleX = actionScale
                                scaleY = actionScale
                            },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play All Button
                        NeumorphicSurface(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("play_all_button"),
                            cornerRadius = 23.dp,
                            elevation = (4.dp * (1f - animatedFraction)).coerceAtLeast(1.dp),
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    playbackViewModel.playTrack(tracks.first(), tracks)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play All",
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Play All",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTheme.colors.textPrimary
                                )
                            }
                        }

                        // Shuffle Button
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("shuffle_playlist_button"),
                            isCircle = true,
                            elevation = (4.dp * (1f - animatedFraction)).coerceAtLeast(1.dp),
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    val shuffled = tracks.shuffled()
                                    playbackViewModel.playTrack(shuffled.first(), shuffled)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = NeumorphicTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Download All Button
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("download_all_button"),
                            isCircle = true,
                            elevation = (4.dp * (1f - animatedFraction)).coerceAtLeast(1.dp),
                            onClick = {
                                tracks.forEach { track ->
                                    homeViewModel.downloadTrack(track)
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar("Downloading ${tracks.size} tracks in background...")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download All",
                                tint = NeumorphicTheme.colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Add Track Button (Custom playlist only)
                        if (!isImported) {
                            NeumorphicSurface(
                                modifier = Modifier
                                    .size(46.dp)
                                    .testTag("add_track_button"),
                                isCircle = true,
                                elevation = (4.dp * (1f - animatedFraction)).coerceAtLeast(1.dp),
                                onClick = { showAddTrackDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Track",
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Item 2: Tracks section header label
                if (tracks.isNotEmpty()) {
                    item(key = "tracks_header_label") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRACKS (${tracks.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTheme.colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = formatTotalDuration(tracks),
                                fontSize = 11.sp,
                                color = NeumorphicTheme.colors.textSecondary
                            )
                        }
                    }
                }

                // Item 3..: Track rows or Empty State
                if (tracks.isEmpty()) {
                    item(key = "empty_tracks_placeholder") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "This playlist is empty",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeumorphicTheme.colors.textPrimary
                                )
                                if (!isImported) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap the + button above to add tracks",
                                        fontSize = 12.sp,
                                        color = NeumorphicTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = displayPlaylistItems,
                        key = { listIndex, item ->
                            when (item) {
                                is ListItemWithAd.Content -> "${item.item.id}_$listIndex"
                                is ListItemWithAd.InlineAd -> "playlist_inline_ad_${item.adIndex}_$listIndex"
                            }
                        }
                    ) { listIndex, displayItem ->
                        when (displayItem) {
                            is ListItemWithAd.Content -> {
                                val track = displayItem.item
                                val rawTrackIndex = tracks.indexOfFirst { it.id == track.id }.let { if (it >= 0) it else listIndex }
                                val isCurrent = playbackState.currentTrack?.id == track.id
                                val isPlaying = isCurrent && playbackState.isPlaying
                                val downloadEntity = downloadedTracks.find { it.trackId == track.id }
                                val isDownloaded = downloadEntity?.status == DownloadStatus.COMPLETED
                                val isDownloading = downloadEntity?.status == DownloadStatus.DOWNLOADING || downloadEntity?.status == DownloadStatus.PENDING

                                PlaylistTrackRow(
                                    index = rawTrackIndex + 1,
                                    track = track,
                                    isCurrent = isCurrent,
                                    isPlaying = isPlaying,
                                    isDownloaded = isDownloaded,
                                    isDownloading = isDownloading,
                                    isCustomPlaylist = !isImported,
                                    canMoveUp = !isImported && rawTrackIndex > 0,
                                    canMoveDown = !isImported && rawTrackIndex < tracks.size - 1,
                                    durationFormatted = playbackViewModel.formatDuration(track.durationSeconds * 1000L),
                                    onPlay = {
                                        if (isCurrent) {
                                            playbackViewModel.togglePlayPause()
                                        } else {
                                            playbackViewModel.playTrack(track, tracks)
                                            homeViewModel.recordTrackPlayed(track)
                                        }
                                    },
                                    onDownload = {
                                        if (!isDownloaded && !isDownloading) {
                                            homeViewModel.downloadTrack(track)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Downloading \"${track.title}\"")
                                            }
                                        }
                                    },
                                    onMoveUp = {
                                        if (!isImported && rawTrackIndex > 0) {
                                            val mutable = tracks.toMutableList()
                                            val item = mutable.removeAt(rawTrackIndex)
                                            mutable.add(rawTrackIndex - 1, item)
                                            homeViewModel.reorderCustomPlaylist(playlistId, mutable)
                                        }
                                    },
                                    onMoveDown = {
                                        if (!isImported && rawTrackIndex < tracks.size - 1) {
                                            val mutable = tracks.toMutableList()
                                            val item = mutable.removeAt(rawTrackIndex)
                                            mutable.add(rawTrackIndex + 1, item)
                                            homeViewModel.reorderCustomPlaylist(playlistId, mutable)
                                        }
                                    },
                                    onRemove = {
                                        if (!isImported) {
                                            homeViewModel.removeTrackFromPlaylist(playlistId, track.id)
                                        }
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

                    // AD UNIT 3 — 300x250 Banner at the end of playlist track list
                    if (canShowFooterAd(tracks.size, INLINE_AD_INTERVAL)) {
                        item(key = "playlist_footer_ad_300x250") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AdBannerView(
                                    adUnit = AdUnit.Banner300x250(),
                                    showContainerCard = true
                                )
                            }
                        }
                    }
                }
            }

            // 2. PINNED NEUMORPHIC TOP BAR (Remains pinned at the top)
            val topBarElevation by animateDpAsState(
                targetValue = if (animatedFraction > 0.05f) 5.dp else 0.dp,
                label = "top_bar_elevation"
            )
            val topBarCornerRadius by animateDpAsState(
                targetValue = if (animatedFraction > 0.05f) 20.dp else 0.dp,
                label = "top_bar_corner"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                if (animatedFraction > 0.05f) {
                    NeumorphicSurface(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = topBarCornerRadius,
                        elevation = topBarElevation
                    ) {
                        CollapsedTopBarContent(
                            title = title,
                            isImported = isImported,
                            animatedFraction = animatedFraction,
                            onNavigateBack = onNavigateBack,
                            onDeleteClick = { showDeleteConfirmDialog = true }
                        )
                    }
                } else {
                    CollapsedTopBarContent(
                        title = title,
                        isImported = isImported,
                        animatedFraction = animatedFraction,
                        onNavigateBack = onNavigateBack,
                        onDeleteClick = { showDeleteConfirmDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedTopBarContent(
    title: String,
    isImported: Boolean,
    animatedFraction: Float,
    onNavigateBack: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeumorphicSurface(
            modifier = Modifier
                .size(40.dp)
                .testTag("playlist_back_button"),
            isCircle = true,
            elevation = 3.dp,
            onClick = onNavigateBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = NeumorphicTheme.colors.textPrimary,
                modifier = Modifier.size(19.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Expanded Subtitle Label ("YouTube Playlist" or "Custom Playlist")
            if (animatedFraction < 0.95f) {
                Text(
                    text = if (isImported) "YouTube Playlist" else "Custom Playlist",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeumorphicTheme.colors.textSecondary.copy(
                        alpha = (1f - (animatedFraction * 1.5f)).coerceIn(0f, 1f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Collapsed Playlist Title ("my favourite song" / "Bollywood Top Hits")
            if (animatedFraction > 0.05f) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicTheme.colors.textPrimary.copy(
                        alpha = ((animatedFraction - 0.2f) / 0.8f).coerceIn(0f, 1f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        NeumorphicSurface(
            modifier = Modifier
                .size(40.dp)
                .testTag("playlist_delete_button"),
            isCircle = true,
            elevation = 3.dp,
            onClick = onDeleteClick
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Playlist",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    index: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isCustomPlaylist: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    durationFormatted: String,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        elevation = if (isCurrent) 2.dp else 4.dp,
        onClick = onPlay
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track number or visual play state
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        modifier = Modifier.size(20.dp)
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
                            barWidth = 2.5.dp,
                            maxHeight = 14.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrent) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "Downloaded",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "${track.artist} • $durationFormatted",
                        fontSize = 11.sp,
                        color = NeumorphicTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Download Status Action
            if (isDownloaded) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Offline ready",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp)
                )
            } else if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = NeumorphicTheme.colors.accent
                )
            } else {
                NeumorphicSurface(
                    modifier = Modifier.size(32.dp),
                    isCircle = true,
                    elevation = 2.dp,
                    onClick = onDownload
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download track",
                        tint = NeumorphicTheme.colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Reorder Buttons (Custom Playlists)
            if (isCustomPlaylist) {
                if (canMoveUp) {
                    NeumorphicSurface(
                        modifier = Modifier.size(30.dp),
                        isCircle = true,
                        elevation = 1.dp,
                        onClick = onMoveUp
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (canMoveDown) {
                    NeumorphicSurface(
                        modifier = Modifier.size(30.dp),
                        isCircle = true,
                        elevation = 1.dp,
                        onClick = onMoveDown
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                NeumorphicSurface(
                    modifier = Modifier.size(30.dp),
                    isCircle = true,
                    elevation = 1.dp,
                    onClick = onRemove
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove track",
                        tint = NeumorphicTheme.colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Play / Pause Button
            NeumorphicSurface(
                modifier = Modifier.size(34.dp),
                isCircle = true,
                elevation = 2.dp,
                onClick = onPlay
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NeumorphicTheme.colors.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddTrackToPlaylistDialog(
    existingTracks: List<Track>,
    onDismiss: () -> Unit,
    onTrackSelected: (Track) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val existingIds = remember(existingTracks) { existingTracks.map { it.id }.toSet() }

    val filteredTracks = remember(searchQuery, existingIds) {
        CuratedTracks.allCurated.filter { track ->
            !existingIds.contains(track.id) &&
                    (searchQuery.isBlank() ||
                            track.title.contains(searchQuery, ignoreCase = true) ||
                            track.artist.contains(searchQuery, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Track to Playlist",
                color = NeumorphicTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs or artists...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeumorphicTheme.colors.accent,
                        unfocusedBorderColor = NeumorphicTheme.colors.surfaceVariant,
                        focusedTextColor = NeumorphicTheme.colors.textPrimary,
                        unfocusedTextColor = NeumorphicTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredTracks) { _, track ->
                        NeumorphicSurface(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 10.dp,
                            elevation = 2.dp,
                            onClick = { onTrackSelected(track) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeumorphicTheme.colors.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = track.thumbnailUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeumorphicTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        fontSize = 10.sp,
                                        color = NeumorphicTheme.colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NeumorphicTheme.colors.accent)
            }
        },
        containerColor = NeumorphicTheme.colors.background
    )
}

private fun formatTotalDuration(tracks: List<Track>): String {
    val totalSeconds = tracks.sumOf { it.durationSeconds }
    val minutes = totalSeconds / 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        "${hours}h ${remainingMinutes}m"
    } else {
        "${minutes} mins"
    }
}
