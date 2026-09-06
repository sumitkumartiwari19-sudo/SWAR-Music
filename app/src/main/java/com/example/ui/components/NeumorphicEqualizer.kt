package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeumorphicTheme

/**
 * 3-bar animated audio equalizer icon.
 * Displays dynamic leaping bars when [isPlaying] is true, or fixed rest bars when false.
 */
@Composable
fun NeumorphicEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = NeumorphicTheme.colors.accent,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 16.dp,
    spacing: Dp = 2.dp
) {
    val transition = rememberInfiniteTransition(label = "EqualizerAnimation")

    val bar1Height by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar1"
    )

    val bar2Height by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar2"
    )

    val bar3Height by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar3"
    )

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Bottom
    ) {
        val h1 = if (isPlaying) (maxHeight * bar1Height).coerceAtLeast(3.dp) else 4.dp
        val h2 = if (isPlaying) (maxHeight * bar2Height).coerceAtLeast(3.dp) else 10.dp
        val h3 = if (isPlaying) (maxHeight * bar3Height).coerceAtLeast(3.dp) else 6.dp

        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h1)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h2)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h3)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
    }
}
