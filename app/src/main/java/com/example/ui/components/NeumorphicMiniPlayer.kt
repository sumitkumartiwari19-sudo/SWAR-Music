package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.playback.PlaybackState
import com.example.ui.theme.NeumorphicTheme

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.ui.animation.PlayerMotion

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NeumorphicMiniPlayer(
    playbackState: PlaybackState,
    isLiked: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleLike: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val track = playbackState.currentTrack

    AnimatedVisibility(
        visible = track != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        if (track != null) {
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

            NeumorphicSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .then(containerSharedModifier)
                    .clickable(onClick = onClick)
                    .testTag("mini_player_container"),
                cornerRadius = 14.dp,
                elevation = 5.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top slim mini progress bar (2dp) along top edge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(NeumorphicTheme.colors.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(playbackState.progress.coerceIn(0f, 1f))
                                .height(2.dp)
                                .background(NeumorphicTheme.colors.accent)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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

                        // Track Album Art / Thumbnail (Circular to match vinyl in Now Playing)
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(42.dp)
                                .then(artSharedModifier)
                                .testTag("mini_player_album_art"),
                            isCircle = true,
                            elevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(NeumorphicTheme.colors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (track.thumbnailUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = track.thumbnailUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = NeumorphicTheme.colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title & Artist
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Text(
                                text = track.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                fontSize = 11.5.sp,
                                color = NeumorphicTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Like Heart Button
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("mini_player_like_button"),
                            isCircle = true,
                            elevation = 3.dp,
                            onClick = onToggleLike
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = if (isLiked) NeumorphicTheme.colors.accent else NeumorphicTheme.colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Play/Pause with Buffering Indicator
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("mini_player_play_pause_button"),
                            isCircle = true,
                            elevation = 4.dp,
                            onClick = onPlayPause
                        ) {
                            if (playbackState.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = NeumorphicTheme.colors.accent
                                )
                            } else {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Skip Next Button
                        NeumorphicSurface(
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("mini_player_next_button"),
                            isCircle = true,
                            elevation = 3.dp,
                            onClick = onSkipNext
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = NeumorphicTheme.colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
