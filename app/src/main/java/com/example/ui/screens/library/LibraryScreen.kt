package com.example.ui.screens.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.YoutubeSearchedFor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.ui.ads.interleaveWithAds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.local.entity.DownloadStatus
import com.example.data.local.entity.DownloadedTrackEntity
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
fun LibraryScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(
        factory = PlaybackViewModel.provideFactory(LocalContext.current)
    ),
    onNavigateToPlaylist: (playlistId: String, isImported: Boolean) -> Unit = { _, _ -> },
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedTab by homeViewModel.selectedLibraryTab.collectAsStateWithLifecycle()
    val tabs = listOf("Playlists", "Downloads", "Liked", "History")
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { tabs.size })

    // Sync pagerState -> homeViewModel when user swipes
    LaunchedEffect(pagerState.currentPage) {
        if (selectedTab != pagerState.currentPage) {
            homeViewModel.setSelectedLibraryTab(pagerState.currentPage)
        }
    }

    // Sync homeViewModel -> pagerState when tab changes
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showImportPlaylistDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    var newPlaylistName by remember { mutableStateOf("") }
    var importUrlInput by remember { mutableStateOf("") }

    val likedSongs by homeViewModel.likedSongs.collectAsStateWithLifecycle()
    val customPlaylists by homeViewModel.customPlaylists.collectAsStateWithLifecycle()
    val importedPlaylists by homeViewModel.importedPlaylists.collectAsStateWithLifecycle()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val downloadedTracks by homeViewModel.downloadedTracks.collectAsStateWithLifecycle()
    val totalStorageBytes by homeViewModel.totalStorageBytes.collectAsStateWithLifecycle()
    val isImporting by homeViewModel.isImportingPlaylist.collectAsStateWithLifecycle()
    val importError by homeViewModel.importError.collectAsStateWithLifecycle()
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 1. Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text(
                    text = "New Custom Playlist",
                    color = NeumorphicTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Create a custom cloud-synced playlist:",
                        fontSize = 13.sp,
                        color = NeumorphicTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("e.g. Midnight Ragas & Lo-Fi") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeumorphicTheme.colors.accent,
                            unfocusedBorderColor = NeumorphicTheme.colors.surfaceVariant,
                            focusedTextColor = NeumorphicTheme.colors.textPrimary,
                            unfocusedTextColor = NeumorphicTheme.colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_playlist_name_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            homeViewModel.createPlaylist(newPlaylistName.trim())
                            val name = newPlaylistName
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Created playlist \"$name\"")
                            }
                        }
                    },
                    modifier = Modifier.testTag("create_playlist_confirm_button")
                ) {
                    Text("Create", color = NeumorphicTheme.colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
                }
            },
            containerColor = NeumorphicTheme.colors.background
        )
    }

    // 2. Import YouTube Playlist Dialog
    if (showImportPlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isImporting) {
                    showImportPlaylistDialog = false
                    homeViewModel.clearImportError()
                }
            },
            title = {
                Text(
                    text = "Import YouTube Playlist",
                    color = NeumorphicTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste a YouTube Playlist URL or Playlist ID (e.g., https://www.youtube.com/playlist?list=PL... or PL...):",
                        fontSize = 12.sp,
                        color = NeumorphicTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importUrlInput,
                        onValueChange = {
                            importUrlInput = it
                            homeViewModel.clearImportError()
                        },
                        placeholder = { Text("https://www.youtube.com/playlist?list=...") },
                        singleLine = false,
                        maxLines = 3,
                        enabled = !isImporting,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeumorphicTheme.colors.accent,
                            unfocusedBorderColor = NeumorphicTheme.colors.surfaceVariant,
                            focusedTextColor = NeumorphicTheme.colors.textPrimary,
                            unfocusedTextColor = NeumorphicTheme.colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_youtube_playlist_input")
                    )

                    if (isImporting) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = NeumorphicTheme.colors.accent
                            )
                            Text(
                                text = "Extracting tracks with NewPipe Extractor...",
                                fontSize = 12.sp,
                                color = NeumorphicTheme.colors.accent
                            )
                        }
                    }

                    if (importError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importError ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importUrlInput.isNotBlank() && !isImporting) {
                            homeViewModel.importYouTubePlaylist(importUrlInput.trim()) { success ->
                                if (success) {
                                    importUrlInput = ""
                                    showImportPlaylistDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("YouTube playlist imported successfully!")
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isImporting && importUrlInput.isNotBlank(),
                    modifier = Modifier.testTag("import_playlist_confirm_button")
                ) {
                    Text(
                        text = if (isImporting) "Importing..." else "Import",
                        color = if (isImporting) NeumorphicTheme.colors.textSecondary else NeumorphicTheme.colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportPlaylistDialog = false
                        homeViewModel.clearImportError()
                    },
                    enabled = !isImporting
                ) {
                    Text("Cancel", color = NeumorphicTheme.colors.textSecondary)
                }
            },
            containerColor = NeumorphicTheme.colors.background
        )
    }

    // 3. Clear All Downloads Dialog
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
                    text = "This will delete all offline audio files from your device storage (${homeViewModel.formatStorageSize(totalStorageBytes)}). You can re-download them anytime.",
                    fontSize = 13.sp,
                    color = NeumorphicTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        homeViewModel.clearAllDownloads()
                        showClearDownloadsDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("All offline downloads cleared")
                        }
                    }
                ) {
                    Text("Clear All", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeumorphicTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Library",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTheme.colors.textPrimary
                    )
                    Text(
                        text = "Playlists, offline downloads & favorites",
                        fontSize = 13.sp,
                        color = NeumorphicTheme.colors.textSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTab == 0) {
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("import_yt_playlist_button"),
                            isCircle = true,
                            elevation = 4.dp,
                            onClick = { showImportPlaylistDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.YoutubeSearchedFor,
                                contentDescription = "Import YouTube Playlist",
                                tint = NeumorphicTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        NeumorphicSurface(
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("create_playlist_button"),
                            isCircle = true,
                            elevation = 4.dp,
                            onClick = { showCreatePlaylistDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Playlist",
                                tint = NeumorphicTheme.colors.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    NeumorphicSurface(
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("library_settings_button"),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Neumorphic Segmented Tab Bar (4 Tabs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, tabTitle ->
                    val isSelected = selectedTab == index
                    if (isSelected) {
                        NeumorphicInsetSurface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    homeViewModel.setSelectedLibraryTab(index)
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                                .testTag("library_tab_$index"),
                            cornerRadius = 19.dp,
                            depth = 3.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicTheme.colors.accent
                                )
                            }
                        }
                    } else {
                        NeumorphicSurface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("library_tab_$index"),
                            cornerRadius = 19.dp,
                            elevation = 3.dp,
                            onClick = {
                                homeViewModel.setSelectedLibraryTab(index)
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeumorphicTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                0 -> {
                    // PLAYLISTS TAB: Showing "Your Playlists" and "Imported Playlists"
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            // AD UNIT 3 — 300x250 Banner above Playlists list on Library screen
                            item(key = "library_playlists_banner_300x250") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AdBannerView(
                                        adUnit = AdUnit.Banner300x250(),
                                        showContainerCard = true
                                    )
                                }
                            }

                            // Section 1: Your Playlists
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "YOUR PLAYLISTS (${customPlaylists.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTheme.colors.textSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "+ New",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTheme.colors.accent,
                                        modifier = Modifier.clickable { showCreatePlaylistDialog = true }
                                    )
                                }
                            }

                            if (customPlaylists.isEmpty()) {
                                item {
                                    NeumorphicSurface(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 14.dp,
                                        elevation = 2.dp,
                                        onClick = { showCreatePlaylistDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = NeumorphicTheme.colors.accent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Create your first playlist",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = NeumorphicTheme.colors.textPrimary
                                                )
                                                Text(
                                                    text = "Organize songs & custom track orders",
                                                    fontSize = 11.sp,
                                                    color = NeumorphicTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(customPlaylists, key = { "cust_${it.id}" }) { playlist ->
                                    PlaylistRowCard(
                                        title = playlist.name,
                                        subtitle = "${playlist.tracks.size} tracks",
                                        isImported = false,
                                        thumbnailUrl = playlist.tracks.firstOrNull()?.thumbnailUrl ?: "",
                                        onClick = { onNavigateToPlaylist(playlist.id, false) },
                                        onDelete = { homeViewModel.deletePlaylist(playlist.id) }
                                    )
                                }
                            }

                            // Section 2: Imported Playlists (From YouTube)
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "IMPORTED FROM YOUTUBE (${importedPlaylists.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTheme.colors.textSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "+ Import",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTheme.colors.accent,
                                        modifier = Modifier.clickable { showImportPlaylistDialog = true }
                                    )
                                }
                            }

                            if (importedPlaylists.isEmpty()) {
                                item {
                                    NeumorphicSurface(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 14.dp,
                                        elevation = 2.dp,
                                        onClick = { showImportPlaylistDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.YoutubeSearchedFor,
                                                contentDescription = null,
                                                tint = NeumorphicTheme.colors.accent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Import a YouTube Playlist",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = NeumorphicTheme.colors.textPrimary
                                                )
                                                Text(
                                                    text = "Paste any YouTube playlist link or ID",
                                                    fontSize = 11.sp,
                                                    color = NeumorphicTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(importedPlaylists, key = { "imp_${it.id}" }) { playlist ->
                                    PlaylistRowCard(
                                        title = playlist.title,
                                        subtitle = "${playlist.tracks.size} tracks • by ${playlist.author}",
                                        isImported = true,
                                        thumbnailUrl = playlist.tracks.firstOrNull()?.thumbnailUrl ?: "",
                                        onClick = { onNavigateToPlaylist(playlist.id, true) },
                                        onDelete = { homeViewModel.deleteImportedPlaylist(playlist.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // DOWNLOADS TAB (Offline Playback)
                    val completed = remember(downloadedTracks) {
                        downloadedTracks.filter { it.status == DownloadStatus.COMPLETED }
                    }
                    val active = remember(downloadedTracks) {
                        downloadedTracks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
                    }
                    val calculatedStorageBytes = remember(completed) {
                        completed.sumOf { it.fileSizeBytes }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            // Storage Info Hero Card
                            item {
                                NeumorphicSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 18.dp,
                                    elevation = 5.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(NeumorphicTheme.colors.accent.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.OfflinePin,
                                                    contentDescription = "Offline Storage",
                                                    tint = NeumorphicTheme.colors.accent,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = "Offline Storage",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeumorphicTheme.colors.textPrimary
                                                )
                                                Text(
                                                    text = "${completed.size} downloaded • ${homeViewModel.formatStorageSize(calculatedStorageBytes)}",
                                                    fontSize = 12.sp,
                                                    color = NeumorphicTheme.colors.textSecondary
                                                )
                                            }
                                        }

                                        if (completed.isNotEmpty() || active.isNotEmpty()) {
                                            NeumorphicSurface(
                                                modifier = Modifier.size(36.dp),
                                                isCircle = true,
                                                elevation = 2.dp,
                                                onClick = { showClearDownloadsDialog = true }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ClearAll,
                                                    contentDescription = "Clear All",
                                                    tint = Color(0xFFE53935),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Active Downloads Section
                            if (active.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ACTIVE DOWNLOADS (${active.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTheme.colors.accent,
                                        letterSpacing = 1.sp
                                    )
                                }

                                items(active, key = { "active_${it.trackId}" }) { downloadItem ->
                                    ActiveDownloadRow(download = downloadItem)
                                }
                            }

                            // Completed Downloads Section
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DOWNLOADED TRACKS (${completed.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicTheme.colors.textSecondary,
                                        letterSpacing = 1.sp
                                    )

                                    if (completed.isNotEmpty()) {
                                        Text(
                                            text = "Play All Offline",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeumorphicTheme.colors.accent,
                                            modifier = Modifier.clickable {
                                                val tracks = completed.map { it.toTrack() }
                                                playbackViewModel.playTrack(tracks.first(), tracks)
                                            }
                                        )
                                    }
                                }
                            }

                            if (completed.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = NeumorphicTheme.colors.accent,
                                                modifier = Modifier.size(46.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "No offline tracks yet",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NeumorphicTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = "Tap the download icon on any song to save offline",
                                                fontSize = 12.sp,
                                                color = NeumorphicTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                val allTracks = completed.map { it.toTrack() }
                                items(completed, key = { "completed_${it.trackId}" }) { item ->
                                    val track = item.toTrack()
                                    val isCurrent = playbackState.currentTrack?.id == track.id
                                    val isPlaying = isCurrent && playbackState.isPlaying

                                    DownloadedTrackRow(
                                        download = item,
                                        isCurrent = isCurrent,
                                        isPlaying = isPlaying,
                                        durationFormatted = playbackViewModel.formatDuration(track.durationSeconds * 1000L),
                                        formattedSize = homeViewModel.formatStorageSize(item.fileSizeBytes),
                                        onPlay = {
                                            if (isCurrent) {
                                                playbackViewModel.togglePlayPause()
                                            } else {
                                                playbackViewModel.playTrack(track, allTracks)
                                            }
                                        },
                                        onDelete = {
                                            homeViewModel.deleteDownloadedTrack(item.trackId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // LIKED SONGS TAB
                    val tracks = if (likedSongs.isNotEmpty()) likedSongs.map { it.track } else CuratedTracks.quickPicks.take(4)
                    val displayLikedTracks = remember(tracks) {
                        tracks.interleaveWithAds(INLINE_AD_INTERVAL)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "${tracks.size} Liked Songs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(
                                items = displayLikedTracks,
                                key = { item ->
                                    when (item) {
                                        is ListItemWithAd.Content -> "liked_${item.item.id}"
                                        is ListItemWithAd.InlineAd -> "liked_inline_ad_${item.adIndex}"
                                    }
                                }
                            ) { displayItem ->
                                when (displayItem) {
                                    is ListItemWithAd.Content -> {
                                        val track = displayItem.item
                                        val isCurrent = playbackState.currentTrack?.id == track.id
                                        val isPlaying = isCurrent && playbackState.isPlaying
                                        val isDownloaded = downloadedTracks.any { it.trackId == track.id && it.status == DownloadStatus.COMPLETED }

                                        TrackListRowWithDownload(
                                            track = track,
                                            isCurrent = isCurrent,
                                            isPlaying = isPlaying,
                                            isDownloaded = isDownloaded,
                                            durationFormatted = playbackViewModel.formatDuration(track.durationSeconds * 1000L),
                                            onPlay = {
                                                if (isCurrent) {
                                                    playbackViewModel.togglePlayPause()
                                                } else {
                                                    playbackViewModel.playTrack(track, tracks)
                                                    homeViewModel.recordTrackPlayed(track)
                                                }
                                            },
                                            onToggleDownload = {
                                                if (!isDownloaded) {
                                                    homeViewModel.downloadTrack(track)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Downloading \"${track.title}\"")
                                                    }
                                                }
                                            },
                                            onRemove = {
                                                homeViewModel.toggleLikedSong(track)
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

                3 -> {
                    // HISTORY TAB
                    val historyTracks = if (recentlyPlayed.isNotEmpty()) {
                        recentlyPlayed.map { it.track }
                    } else {
                        CuratedTracks.allCurated
                    }
                    val displayHistoryTracks = remember(historyTracks) {
                        historyTracks.interleaveWithAds(INLINE_AD_INTERVAL)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "${historyTracks.size} items in playback history",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            itemsIndexed(
                                items = displayHistoryTracks,
                                key = { index, item ->
                                    when (item) {
                                        is ListItemWithAd.Content -> "hist_${item.item.id}_$index"
                                        is ListItemWithAd.InlineAd -> "hist_inline_ad_${item.adIndex}_$index"
                                    }
                                }
                            ) { _, displayItem ->
                                when (displayItem) {
                                    is ListItemWithAd.Content -> {
                                        val track = displayItem.item
                                        val isCurrent = playbackState.currentTrack?.id == track.id
                                        val isPlaying = isCurrent && playbackState.isPlaying
                                        val isDownloaded = downloadedTracks.any { it.trackId == track.id && it.status == DownloadStatus.COMPLETED }

                                        TrackListRowWithDownload(
                                            track = track,
                                            isCurrent = isCurrent,
                                            isPlaying = isPlaying,
                                            isDownloaded = isDownloaded,
                                            durationFormatted = playbackViewModel.formatDuration(track.durationSeconds * 1000L),
                                            onPlay = {
                                                if (isCurrent) {
                                                    playbackViewModel.togglePlayPause()
                                                } else {
                                                    playbackViewModel.playTrack(track, historyTracks)
                                                    homeViewModel.recordTrackPlayed(track)
                                                }
                                            },
                                            onToggleDownload = {
                                                if (!isDownloaded) {
                                                    homeViewModel.downloadTrack(track)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Downloading \"${track.title}\"")
                                                    }
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
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PlaylistRowCard(
    title: String,
    subtitle: String,
    isImported: Boolean,
    thumbnailUrl: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = 4.dp,
        onClick = onClick
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeumorphicTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (isImported) Icons.Default.YoutubeSearchedFor else Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeumorphicTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = NeumorphicTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NeumorphicSurface(
                modifier = Modifier.size(34.dp),
                isCircle = true,
                elevation = 2.dp,
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = NeumorphicTheme.colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveDownloadRow(download: DownloadedTrackEntity) {
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = NeumorphicTheme.colors.accent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = download.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeumorphicTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${download.progress}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicTheme.colors.accent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (download.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = NeumorphicTheme.colors.accent,
                trackColor = NeumorphicTheme.colors.surfaceVariant
            )
        }
    }
}

@Composable
private fun DownloadedTrackRow(
    download: DownloadedTrackEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    durationFormatted: String,
    formattedSize: String,
    onPlay: () -> Unit,
    onDelete: () -> Unit
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeumorphicTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (download.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = download.thumbnailUrl,
                        contentDescription = download.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeumorphicTheme.colors.accent,
                        modifier = Modifier.size(22.dp)
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
                    text = download.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrent) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Offline ready",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${download.artist} • $durationFormatted • $formattedSize",
                        fontSize = 11.sp,
                        color = NeumorphicTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NeumorphicSurface(
                modifier = Modifier.size(32.dp),
                isCircle = true,
                elevation = 2.dp,
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete downloaded audio",
                    tint = NeumorphicTheme.colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

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
private fun TrackListRowWithDownload(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    durationFormatted: String,
    onPlay: () -> Unit,
    onToggleDownload: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                        modifier = Modifier.size(22.dp)
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
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Offline ready",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(11.dp)
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

            if (onToggleDownload != null && !isDownloaded) {
                NeumorphicSurface(
                    modifier = Modifier.size(32.dp),
                    isCircle = true,
                    elevation = 2.dp,
                    onClick = onToggleDownload
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download track",
                        tint = NeumorphicTheme.colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (onRemove != null) {
                NeumorphicSurface(
                    modifier = Modifier.size(32.dp),
                    isCircle = true,
                    elevation = 2.dp,
                    onClick = onRemove
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Remove from favorites",
                        tint = NeumorphicTheme.colors.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

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
