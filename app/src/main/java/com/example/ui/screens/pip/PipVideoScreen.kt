package com.example.ui.screens.pip

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.playback.PlaybackState
import com.example.ui.theme.NeumorphicTheme

/**
 * Minimalist full-bleed video surface rendered exclusively while in Picture-in-Picture mode.
 * Contains no UI chrome, neumorphic containers, or navigation controls, adhering strictly
 * to Android PiP floating window guidelines.
 */
@Composable
fun PipVideoScreen(
    exoPlayer: ExoPlayer,
    playbackState: PlaybackState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("pip_video_surface"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
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
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playbackState.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp)
                    .testTag("pip_buffering_indicator"),
                strokeWidth = 2.5.dp,
                color = NeumorphicTheme.colors.accent
            )
        }
    }
}
