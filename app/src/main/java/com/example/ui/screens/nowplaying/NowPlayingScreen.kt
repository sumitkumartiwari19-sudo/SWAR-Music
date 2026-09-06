package com.example.ui.screens.nowplaying

import android.content.Intent
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.ImeAction
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.entity.DownloadStatus
import com.example.data.model.CuratedTracks
import com.example.playback.PlaybackMode
import com.example.playback.RepeatMode
import com.example.playback.VideoQuality
import com.example.ui.components.NeumorphicEqualizer
import com.example.ui.components.NeumorphicInsetSurface
import com.example.ui.components.NeumorphicSurface
import com.example.ui.playback.PlaybackViewModel
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.theme.NeumorphicTheme
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.animation.PlayerMotion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(
        factory = PlaybackViewModel.provideFactory(LocalContext.current)
    ),
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val state by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val isLiked by playbackViewModel.isCurrentTrackLiked.collectAsStateWithLifecycle()
    val downloadedTracks by homeViewModel.downloadedTracks.collectAsStateWithLifecycle()
    val customPlaylists by homeViewModel.customPlaylists.collectAsStateWithLifecycle()

    val currentTrackDownload = state.currentTrack?.let { current ->
        downloadedTracks.find { it.trackId == current.id }
    }
    val isDownloaded = currentTrackDownload?.status == DownloadStatus.COMPLETED
    val isDownloading = currentTrackDownload?.status == DownloadStatus.DOWNLOADING || currentTrackDownload?.status == DownloadStatus.PENDING

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderPosition by remember { mutableFloatStateOf(0f) }

    var showLyricsSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSaveToPlaylistSheet by remember { mutableStateOf(false) }
    var showVideoQualitySheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Video Quality Selection Bottom Sheet
    if (showVideoQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showVideoQualitySheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = NeumorphicTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Video Quality",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTheme.colors.textPrimary
                        )
                        Text(
                            text = "Select preferred streaming resolution",
                            fontSize = 12.sp,
                            color = NeumorphicTheme.colors.textSecondary
                        )
                    }
                    NeumorphicSurface(
                        modifier = Modifier.size(36.dp),
                        isCircle = true,
                        elevation = 2.dp,
                        onClick = { showVideoQualitySheet = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val qualities = listOf(
                        VideoQuality.AUTO,
                        VideoQuality.Q1080P,
                        VideoQuality.Q720P,
                        VideoQuality.Q480P
                    )

                    qualities.forEach { quality ->
                        val isSelected = state.videoQuality == quality
                        NeumorphicSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("video_quality_option_${quality.name.lowercase()}"),
                            cornerRadius = 14.dp,
                            elevation = if (isSelected) 1.dp else 4.dp,
                            onClick = {
                                playbackViewModel.setVideoQuality(quality)
                                showVideoQualitySheet = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Video quality set to ${quality.label}")
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = if (isSelected) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = quality.label,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary
                                        )
                                        if (quality == VideoQuality.AUTO) {
                                            Text(
                                                text = "Adapts dynamically to network speed",
                                                fontSize = 11.sp,
                                                color = NeumorphicTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = NeumorphicTheme.colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Lyrics Bottom Sheet
    if (showLyricsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = NeumorphicTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lyrics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTheme.colors.textPrimary
                    )
                    NeumorphicSurface(
                        modifier = Modifier.size(36.dp),
                        isCircle = true,
                        elevation = 2.dp,
                        onClick = { showLyricsSheet = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NeumorphicInsetSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    cornerRadius = 16.dp,
                    depth = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.currentTrack?.title ?: "Now Playing",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTheme.colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = state.currentTrack?.artist ?: "",
                            fontSize = 13.sp,
                            color = NeumorphicTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = """
                                ♫ Swara and Tala weaving in harmony...
                                Echoes of ragas floating through the dusk.
                                Sitar strings resonate across the gentle twilight,
                                Carrying memories of ancient melodies.
                                
                                Live synchronized lyrics supported by SWAR.
                            """.trimIndent(),
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            color = NeumorphicTheme.colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = NeumorphicTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Up Next Queue",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTheme.colors.textPrimary
                        )
                        Text(
                            text = "${state.queue.size} songs in playlist",
                            fontSize = 12.sp,
                            color = NeumorphicTheme.colors.textSecondary
                        )
                    }
                    NeumorphicSurface(
                        modifier = Modifier.size(36.dp),
                        isCircle = true,
                        elevation = 2.dp,
                        onClick = { showQueueSheet = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(state.queue) { index, track ->
                        val isCurrent = index == state.currentIndex
                        NeumorphicSurface(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp,
                            elevation = if (isCurrent) 1.dp else 3.dp,
                            onClick = {
                                playbackViewModel.playTrack(track, state.queue)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = track.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrent) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            fontSize = 11.sp,
                                            color = NeumorphicTheme.colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isCurrent && state.isPlaying) {
                                    NeumorphicEqualizer(
                                        isPlaying = true,
                                        barColor = NeumorphicTheme.colors.accent,
                                        barWidth = 2.5.dp,
                                        maxHeight = 14.dp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Save to Playlist Bottom Sheet
    if (showSaveToPlaylistSheet && state.currentTrack != null) {
        val currentTrack = state.currentTrack!!
        var newPlaylistName by remember { mutableStateOf("") }
        var isCreatingNew by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showSaveToPlaylistSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = NeumorphicTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Save to Playlist",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTheme.colors.textPrimary
                        )
                        Text(
                            text = "Add \"${currentTrack.title}\" to your library",
                            fontSize = 12.sp,
                            color = NeumorphicTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    NeumorphicSurface(
                        modifier = Modifier.size(36.dp),
                        isCircle = true,
                        elevation = 2.dp,
                        onClick = { showSaveToPlaylistSheet = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inline "Create New Playlist" toggle / field
                if (!isCreatingNew) {
                    NeumorphicSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_new_playlist_button"),
                        cornerRadius = 14.dp,
                        elevation = 3.dp,
                        onClick = { isCreatingNew = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Playlist",
                                tint = NeumorphicTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "New Playlist",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTheme.colors.textPrimary
                            )
                        }
                    }
                } else {
                    NeumorphicSurface(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        elevation = 3.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("Playlist Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeumorphicTheme.colors.accent,
                                    unfocusedBorderColor = NeumorphicTheme.colors.textSecondary.copy(alpha = 0.5f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newPlaylistName.isNotBlank()) {
                                        homeViewModel.createPlaylist(
                                            name = newPlaylistName.trim(),
                                            initialTracks = listOf(currentTrack)
                                        )
                                        showSaveToPlaylistSheet = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Created \"${newPlaylistName.trim()}\" & saved track")
                                        }
                                    }
                                })
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    isCreatingNew = false
                                    newPlaylistName = ""
                                }) {
                                    Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                NeumorphicSurface(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("confirm_create_playlist_btn"),
                                    cornerRadius = 19.dp,
                                    elevation = 3.dp,
                                    onClick = {
                                        if (newPlaylistName.isNotBlank()) {
                                            homeViewModel.createPlaylist(
                                                name = newPlaylistName.trim(),
                                                initialTracks = listOf(currentTrack)
                                            )
                                            showSaveToPlaylistSheet = false
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Created \"${newPlaylistName.trim()}\" & saved track")
                                            }
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Create & Save",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeumorphicTheme.colors.accent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Existing playlists list
                if (customPlaylists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No playlists found yet. Create one above to save this song!",
                            fontSize = 13.sp,
                            color = NeumorphicTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(customPlaylists) { _, playlist ->
                            val alreadyInPlaylist = playlist.tracks.any { it.id == currentTrack.id }
                            NeumorphicSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playlist_item_${playlist.id}"),
                                cornerRadius = 12.dp,
                                elevation = if (alreadyInPlaylist) 1.dp else 3.dp,
                                onClick = {
                                    if (!alreadyInPlaylist) {
                                        homeViewModel.addTrackToPlaylist(playlist.id, currentTrack)
                                        showSaveToPlaylistSheet = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Added \"${currentTrack.title}\" to ${playlist.name}")
                                        }
                                    } else {
                                        showSaveToPlaylistSheet = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Already in ${playlist.name}")
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = if (alreadyInPlaylist) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = playlist.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NeumorphicTheme.colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${playlist.tracks.size} tracks",
                                                fontSize = 11.sp,
                                                color = NeumorphicTheme.colors.textSecondary
                                            )
                                        }
                                    }

                                    if (alreadyInPlaylist) {
                                        Text(
                                            text = "Added",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NeumorphicTheme.colors.accent
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add to playlist",
                                            tint = NeumorphicTheme.colors.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    val containerSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = PlayerMotion.SHARED_CONTAINER_KEY),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = PlayerMotion.PlayerBoundsTransform
            )
        }
    } else {
        Modifier
    }

    val artSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(key = PlayerMotion.SHARED_ART_KEY),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = PlayerMotion.PlayerBoundsTransform,
                clipInOverlayDuringTransition = OverlayClip(CircleShape)
            )
        }
    } else {
        Modifier
    }

    val chromeTransition = animatedVisibilityScope?.transition
    val chromeAlpha by if (chromeTransition != null) {
        chromeTransition.animateFloat(
            transitionSpec = {
                if (EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible) {
                    tween(durationMillis = 220, delayMillis = 120, easing = FastOutSlowInEasing)
                } else {
                    tween(durationMillis = 140, easing = FastOutSlowInEasing)
                }
            },
            label = "nowPlayingChromeAlpha"
        ) { targetState ->
            if (targetState == EnterExitState.Visible) 1f else 0f
        }
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val chromeTranslationY by if (chromeTransition != null) {
        chromeTransition.animateFloat(
            transitionSpec = {
                if (EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible) {
                    tween(durationMillis = 240, delayMillis = 100, easing = FastOutSlowInEasing)
                } else {
                    tween(durationMillis = 140, easing = FastOutSlowInEasing)
                }
            },
            label = "nowPlayingChromeTranslationY"
        ) { targetState ->
            if (targetState == EnterExitState.Visible) 0f else 24f
        }
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val chromeModifier = Modifier.graphicsLayer {
        alpha = chromeAlpha
        translationY = chromeTranslationY
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(containerSharedModifier),
        containerColor = NeumorphicTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .displayCutoutPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP BAR: Back button (left), Audio/Video toggle pill (center), Actions (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(chromeModifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Far Left: Back Button
                Box(
                    modifier = Modifier.weight(1f, fill = false),
                    contentAlignment = Alignment.CenterStart
                ) {
                    NeumorphicSurface(
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("now_playing_back_button"),
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
                }

                // Center: Audio / Video Segmented Pill Toggle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    NeumorphicSurface(
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("audio_video_toggle_pill"),
                        cornerRadius = 19.dp,
                        elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAudio = state.playbackMode == PlaybackMode.AUDIO
                            val isVideo = state.playbackMode == PlaybackMode.VIDEO

                            // Audio Mode Segment
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(
                                        if (isAudio) Modifier.background(NeumorphicTheme.colors.surfaceVariant)
                                        else Modifier
                                    )
                                    .clickable {
                                        playbackViewModel.setPlaybackMode(PlaybackMode.AUDIO)
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = "Audio Mode",
                                        tint = if (isAudio) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Song",
                                        fontSize = 12.sp,
                                        fontWeight = if (isAudio) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAudio) NeumorphicTheme.colors.textPrimary else NeumorphicTheme.colors.textSecondary
                                    )
                                }
                            }

                            // Video Mode Segment
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(
                                        if (isVideo) Modifier.background(NeumorphicTheme.colors.surfaceVariant)
                                        else Modifier
                                    )
                                    .clickable {
                                        playbackViewModel.setPlaybackMode(PlaybackMode.VIDEO)
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartDisplay,
                                        contentDescription = "Video Mode",
                                        tint = if (isVideo) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Video",
                                        fontSize = 12.sp,
                                        fontWeight = if (isVideo) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isVideo) NeumorphicTheme.colors.textPrimary else NeumorphicTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Far Right: Actions (PiP if video + 3-Dot Overflow Menu)
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.playbackMode == PlaybackMode.VIDEO) {
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("now_playing_pip_button"),
                            isCircle = true,
                            elevation = 4.dp,
                            onClick = onEnterPip
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                tint = NeumorphicTheme.colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // 3-Dot Overflow Menu Button
                    Box {
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("now_playing_overflow_button"),
                            isCircle = true,
                            elevation = 4.dp,
                            onClick = { showOverflowMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = NeumorphicTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier.background(NeumorphicTheme.colors.background)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = NeumorphicTheme.colors.accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Up Next Queue",
                                            color = NeumorphicTheme.colors.textPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showQueueSheet = true
                                }
                            )

                            if (state.playbackMode == PlaybackMode.VIDEO) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.PictureInPictureAlt,
                                                contentDescription = null,
                                                tint = NeumorphicTheme.colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Picture in Picture",
                                                color = NeumorphicTheme.colors.textPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onEnterPip()
                                    },
                                    modifier = Modifier.testTag("overflow_pip_menu_item")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.HighQuality,
                                                contentDescription = null,
                                                tint = NeumorphicTheme.colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Video Quality (${state.videoQuality.label})",
                                                color = NeumorphicTheme.colors.textPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showVideoQualitySheet = true
                                    },
                                    modifier = Modifier.testTag("overflow_video_quality_menu_item")
                                )
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = NeumorphicTheme.colors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Share Track",
                                            color = NeumorphicTheme.colors.textPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    val current = state.currentTrack
                                    val shareText = if (current != null) {
                                        "Listening to ${current.title} by ${current.artist} on SWAR Music!"
                                    } else {
                                        "Listen to classical and trending music on SWAR Music!"
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Track")
                                    context.startActivity(shareIntent)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. ALBUM ART OR VIDEO PLAYER DISPLAY AREA
            if (state.playbackMode == PlaybackMode.VIDEO) {
                // Video Player Container
                NeumorphicSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .then(artSharedModifier)
                        .testTag("video_player_container"),
                    cornerRadius = 20.dp,
                    elevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = playbackViewModel.exoPlayer
                                    useController = false
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { playerView ->
                                playerView.player = playbackViewModel.exoPlayer
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (state.isBuffering) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp,
                                    color = NeumorphicTheme.colors.accent
                                )
                            }
                        }
                    }
                }
            } else {
                // Audio Mode: Raised circular vinyl artwork (Right-side up)
                NeumorphicSurface(
                    modifier = Modifier
                        .size(260.dp)
                        .then(artSharedModifier)
                        .clip(CircleShape)
                        .testTag("album_art_container"),
                    isCircle = true,
                    elevation = 10.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer vinyl grooves
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(NeumorphicTheme.colors.surfaceVariant)
                        )

                        // Middle Art Thumbnail (displayed right-side-up, strictly no inverted rotation)
                        val track = state.currentTrack
                        if (track?.thumbnailUrl?.isNotBlank() == true) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(176.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeumorphicTheme.colors.accent,
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        // Center vinyl spindle hole
                        NeumorphicInsetSurface(
                            modifier = Modifier.size(44.dp),
                            isCircle = true,
                            depth = 3.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(NeumorphicTheme.colors.accent)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. TRACK TITLE AND ARTIST
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(chromeModifier)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.currentTrack?.title ?: "No Track Playing",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.currentTrack?.artist ?: "Select a song from Home or Search",
                    fontSize = 14.sp,
                    color = NeumorphicTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Error Banner if stream extraction failed
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                NeumorphicSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(chromeModifier),
                    cornerRadius = 12.dp,
                    elevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.errorMessage ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFE53935),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        NeumorphicSurface(
                            modifier = Modifier.size(36.dp),
                            isCircle = true,
                            elevation = 2.dp,
                            onClick = { playbackViewModel.retry() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = NeumorphicTheme.colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. INSET PROGRESS BAR WITH ACCENT-COLORED FILL
            val sliderValue = if (isDraggingSlider) dragSliderPosition else state.progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(chromeModifier)
            ) {
                // Inset Track for the Slider
                NeumorphicInsetSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    cornerRadius = 6.dp,
                    depth = 3.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sliderValue.coerceIn(0.01f, 1f))
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeumorphicTheme.colors.accent)
                        )
                    }
                }

                // Transparent slider overlay for touch scrubbing
                Slider(
                    value = sliderValue,
                    onValueChange = { fraction ->
                        isDraggingSlider = true
                        dragSliderPosition = fraction
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        playbackViewModel.seekToFraction(dragSliderPosition)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NeumorphicTheme.colors.accent,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_progress_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentPos = if (isDraggingSlider) {
                        (dragSliderPosition * state.durationMs).toLong()
                    } else {
                        state.currentPositionMs
                    }
                    Text(
                        text = playbackViewModel.formatDuration(currentPos),
                        fontSize = 12.sp,
                        color = NeumorphicTheme.colors.textSecondary
                    )
                    Text(
                        text = playbackViewModel.formatDuration(state.durationMs),
                        fontSize = 12.sp,
                        color = NeumorphicTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. TRANSPORT CONTROLS: Shuffle, Previous, Large Raised Play/Pause, Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(chromeModifier),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                NeumorphicSurface(
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("shuffle_button"),
                    isCircle = true,
                    elevation = if (state.shuffleEnabled) 1.dp else 4.dp,
                    onClick = { playbackViewModel.toggleShuffle() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleEnabled) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous Button
                NeumorphicSurface(
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("previous_track_button"),
                    isCircle = true,
                    elevation = 5.dp,
                    onClick = { playbackViewModel.skipPrevious() }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = NeumorphicTheme.colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Large Raised Circular Play/Pause Button
                NeumorphicSurface(
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("main_play_pause_button"),
                    isCircle = true,
                    elevation = 8.dp,
                    onClick = { playbackViewModel.togglePlayPause() }
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = NeumorphicTheme.colors.accent
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Next Button
                NeumorphicSurface(
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("next_track_button"),
                    isCircle = true,
                    elevation = 5.dp,
                    onClick = { playbackViewModel.skipNext() }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = NeumorphicTheme.colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Repeat Button
                NeumorphicSurface(
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("repeat_mode_button"),
                    isCircle = true,
                    elevation = if (state.repeatMode != RepeatMode.OFF) 1.dp else 4.dp,
                    onClick = { playbackViewModel.toggleRepeat() }
                ) {
                    val repeatIcon = when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. SECONDARY ACTION ROW: 4 PILLS (Like, Lyrics, Save, Download)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(chromeModifier)
                    .testTag("now_playing_action_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Like Pill
                NeumorphicSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("now_playing_like_button"),
                    cornerRadius = 22.dp,
                    elevation = if (isLiked) 2.dp else 4.dp,
                    onClick = { playbackViewModel.toggleLikeCurrentTrack() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) Color(0xFFE53935) else NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLiked) "Liked" else "Like",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLiked) NeumorphicTheme.colors.textPrimary else NeumorphicTheme.colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }

                // 2. Lyrics Pill
                NeumorphicSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("now_playing_lyrics_button"),
                    cornerRadius = 22.dp,
                    elevation = 4.dp,
                    onClick = { showLyricsSheet = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Lyrics",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lyrics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTheme.colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }

                // 3. Save Pill
                NeumorphicSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("now_playing_save_button"),
                    cornerRadius = 22.dp,
                    elevation = 4.dp,
                    onClick = {
                        if (state.currentTrack != null) {
                            showSaveToPlaylistSheet = true
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("No track playing")
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Save to playlist",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTheme.colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }

                // 4. Download Pill
                val downloadIcon = when {
                    isDownloaded -> Icons.Default.DownloadDone
                    isDownloading -> Icons.Default.Downloading
                    else -> Icons.Default.Download
                }
                val downloadLabel = when {
                    isDownloaded -> "Saved"
                    isDownloading -> "Saving"
                    else -> "Download"
                }

                NeumorphicSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("now_playing_download_button"),
                    cornerRadius = 22.dp,
                    elevation = if (isDownloaded) 2.dp else 4.dp,
                    onClick = {
                        val track = state.currentTrack
                        if (track != null) {
                            if (!isDownloaded && !isDownloading) {
                                homeViewModel.downloadTrack(track)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Downloading \"${track.title}\" for offline playback")
                                }
                            } else if (isDownloaded) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Already downloaded for offline playback")
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Download in progress...")
                                }
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = downloadIcon,
                            contentDescription = "Download",
                            tint = if (isDownloaded) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = downloadLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDownloaded) NeumorphicTheme.colors.textPrimary else NeumorphicTheme.colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
