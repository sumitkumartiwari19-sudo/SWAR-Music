package com.example.ui.screens.search

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import com.example.playback.PlaybackOrigin
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.example.ui.ads.AdBannerView
import com.example.ui.ads.AdUnit
import com.example.ui.ads.INLINE_AD_INTERVAL
import com.example.ui.ads.ListItemWithAd
import com.example.ui.ads.canShowFooterAd
import com.example.ui.ads.interleaveWithAds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.local.entity.DownloadStatus
import com.example.data.model.Track
import com.example.ui.components.NeumorphicEqualizer
import com.example.ui.components.NeumorphicInsetSurface
import com.example.ui.components.NeumorphicSurface
import com.example.ui.playback.PlaybackViewModel
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.theme.NeumorphicTheme
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(
        factory = PlaybackViewModel.provideFactory(LocalContext.current)
    ),
    modifier: Modifier = Modifier
) {
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val downloadedTracks by homeViewModel.downloadedTracks.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // 1. HEADER - Firmly anchored to TOP-LEFT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Search & Discover",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTheme.colors.textPrimary,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "YouTube audio streaming & classical collection",
                        fontSize = 12.sp,
                        color = NeumorphicTheme.colors.textSecondary,
                        textAlign = TextAlign.Start
                    )
                }

                if (uiState.isOnlineResult) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeumorphicTheme.colors.accent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Online Stream",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Live Stream",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTheme.colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. SEARCH INPUT BAR (Neumorphic Inset)
            NeumorphicInsetSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("search_text_input_container"),
                cornerRadius = 26.dp,
                depth = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = NeumorphicTheme.colors.accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = uiState.query,
                        onValueChange = { searchViewModel.onQueryChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_text_field"),
                        singleLine = true,
                        cursorBrush = SolidColor(NeumorphicTheme.colors.accent),
                        textStyle = TextStyle(
                            color = NeumorphicTheme.colors.textPrimary,
                            fontSize = 14.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                searchViewModel.performLiveSearch()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (uiState.query.isEmpty()) {
                                Text(
                                    text = "Search any song, artist, or YouTube URL...",
                                    fontSize = 13.sp,
                                    color = NeumorphicTheme.colors.textSecondary
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (uiState.query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    searchViewModel.clearSearch()
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. CATEGORY FILTER CHIPS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(searchViewModel.categories.size) { index ->
                    val isSelected = uiState.selectedCategoryIndex == index
                    if (isSelected) {
                        NeumorphicSurface(
                            modifier = Modifier.height(34.dp),
                            cornerRadius = 17.dp,
                            elevation = 3.dp,
                            backgroundColor = NeumorphicTheme.colors.accent,
                            onClick = { searchViewModel.onCategorySelect(index) }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = searchViewModel.categories[index],
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        NeumorphicSurface(
                            modifier = Modifier.height(34.dp),
                            cornerRadius = 17.dp,
                            elevation = 3.dp,
                            onClick = { searchViewModel.onCategorySelect(index) }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = searchViewModel.categories[index],
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeumorphicTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic Notice / Error Banner
            uiState.searchError?.let { err ->
                NeumorphicSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    cornerRadius = 12.dp,
                    elevation = 2.dp,
                    backgroundColor = NeumorphicTheme.colors.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Notice",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            fontSize = 11.sp,
                            color = NeumorphicTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = NeumorphicTheme.colors.accent,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchViewModel.performLiveSearch() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Results Counter
            Text(
                text = "${uiState.searchResults.size} audio tracks found ${if (uiState.isOnlineResult) "(via live YouTube stream)" else ""}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = NeumorphicTheme.colors.textSecondary
            )

            // AD UNIT 1 — Persistent 320x50 Banner (below search bar / chips, above search results)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AdBannerView(
                    adUnit = AdUnit.Banner320x50(),
                    showContainerCard = false
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. SEARCH RESULTS LIST
            if (uiState.searchResults.isEmpty() && !uiState.isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NeumorphicTheme.colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tracks found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching for a different song or artist",
                            fontSize = 12.sp,
                            color = NeumorphicTheme.colors.textSecondary
                        )
                    }
                }
            } else {
                val displayResults = remember(uiState.searchResults) {
                    uiState.searchResults.interleaveWithAds(INLINE_AD_INTERVAL)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = displayResults,
                        key = { item ->
                            when (item) {
                                is ListItemWithAd.Content -> item.item.id
                                is ListItemWithAd.InlineAd -> "search_inline_ad_${item.adIndex}"
                            }
                        }
                    ) { displayItem ->
                        when (displayItem) {
                            is ListItemWithAd.Content -> {
                                val track = displayItem.item
                                val isCurrent = playbackState.currentTrack?.id == track.id
                                val isPlaying = isCurrent && playbackState.isPlaying
                                val isBuffering = isCurrent && playbackState.isBuffering
                                val downloadEntity = downloadedTracks.find { it.trackId == track.id }
                                val isDownloaded = downloadEntity?.status == DownloadStatus.COMPLETED
                                val isDownloading = downloadEntity?.status == DownloadStatus.DOWNLOADING || downloadEntity?.status == DownloadStatus.PENDING

                                SearchResultRowItem(
                                    track = track,
                                    isCurrent = isCurrent,
                                    isPlaying = isPlaying,
                                    isBuffering = isBuffering,
                                    isDownloaded = isDownloaded,
                                    isDownloading = isDownloading,
                                    formattedDuration = if (track.durationSeconds > 0) playbackViewModel.formatDuration(track.durationSeconds * 1000L) else "Live",
                                    onRowClick = {
                                        Log.d("SearchScreen", "[AudioTrace:Step-A] Tapped search result track id='${track.id}', title='${track.title}'")
                                        if (isCurrent) {
                                            playbackViewModel.togglePlayPause()
                                        } else {
                                            playbackViewModel.playTrack(track, origin = PlaybackOrigin.SEARCH)
                                            homeViewModel.recordTrackPlayed(track)
                                        }
                                    },
                                    onPlayPauseClick = {
                                        if (isCurrent) {
                                            playbackViewModel.togglePlayPause()
                                        } else {
                                            playbackViewModel.playTrack(track, origin = PlaybackOrigin.SEARCH)
                                            homeViewModel.recordTrackPlayed(track)
                                        }
                                    },
                                    onDownloadClick = {
                                        if (!isDownloaded && !isDownloading) {
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

                    // AD UNIT 3 — 300x250 Banner at the end of search results
                    if (canShowFooterAd(uiState.searchResults.size, INLINE_AD_INTERVAL)) {
                        item(key = "search_footer_ad_300x250") {
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
        }
    }
}

/**
 * Clean Neumorphic Search Result Row with fixed thumbnail, balanced text column,
 * and neatly aligned trailing action buttons.
 */
@Composable
private fun SearchResultRowItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    formattedDuration: String,
    onRowClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeumorphicSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .testTag("search_result_${track.id}"),
        cornerRadius = 16.dp,
        elevation = if (isCurrent) 2.dp else 4.dp,
        contentAlignment = Alignment.CenterStart,
        onClick = onRowClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed-size Album Artwork / Thumbnail Box (56dp)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White
                        )
                    }
                } else if (isPlaying) {
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
                            maxHeight = 16.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Weighted Text Column (Title + Artist/Duration)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrent) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "${track.artist} • $formattedDuration",
                        fontSize = 12.sp,
                        color = NeumorphicTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Trailing Action Buttons Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Download Action Button
                if (isDownloaded) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Offline ready",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (isDownloading) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = NeumorphicTheme.colors.accent
                        )
                    }
                } else {
                    NeumorphicSurface(
                        modifier = Modifier.size(36.dp),
                        isCircle = true,
                        elevation = 2.dp,
                        onClick = onDownloadClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download track",
                            tint = NeumorphicTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Play / Pause Action Button
                NeumorphicSurface(
                    modifier = Modifier.size(38.dp),
                    isCircle = true,
                    elevation = 2.dp,
                    onClick = onPlayPauseClick
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
}

