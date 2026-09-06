package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeumorphicTheme

/**
 * Raised/Extruded Neumorphic Surface.
 * Renders dual soft shadows (light side top-left, dark side bottom-right)
 * against a soft UI background canvas.
 */
@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isCircle: Boolean = false,
    elevation: Dp = 6.dp,
    backgroundColor: Color = NeumorphicTheme.colors.background,
    lightShadowColor: Color = NeumorphicTheme.colors.raisedShadowLight,
    darkShadowColor: Color = NeumorphicTheme.colors.raisedShadowDark,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val elevationPx = with(density) { elevation.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val blurRadius = (elevationPx * 1.4f).coerceAtLeast(1f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset = if (isPressed && onClick != null) elevationPx * 0.4f else elevationPx

    val shape = if (isCircle) CircleShape else RoundedCornerShape(cornerRadius)

    val shadowModifier = Modifier.drawBehind {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@drawBehind

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // 1. Top-Left Light Shadow
            val lightPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = lightShadowColor.toArgb()
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
            nativeCanvas.save()
            nativeCanvas.translate(-currentOffset, -currentOffset)
            if (isCircle) {
                val radius = (width.coerceAtMost(height)) / 2f
                nativeCanvas.drawCircle(width / 2f, height / 2f, radius, lightPaint)
            } else {
                nativeCanvas.drawRoundRect(
                    0f, 0f, width, height,
                    cornerRadiusPx, cornerRadiusPx,
                    lightPaint
                )
            }
            nativeCanvas.restore()

            // 2. Bottom-Right Dark Shadow
            val darkPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = darkShadowColor.toArgb()
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
            nativeCanvas.save()
            nativeCanvas.translate(currentOffset, currentOffset)
            if (isCircle) {
                val radius = (width.coerceAtMost(height)) / 2f
                nativeCanvas.drawCircle(width / 2f, height / 2f, radius, darkPaint)
            } else {
                nativeCanvas.drawRoundRect(
                    0f, 0f, width, height,
                    cornerRadiusPx, cornerRadiusPx,
                    darkPaint
                )
            }
            nativeCanvas.restore()
        }
    }

    Box(
        modifier = modifier
            .then(shadowModifier)
            .clip(shape)
            .background(backgroundColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = NeumorphicTheme.colors.accent),
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = contentAlignment,
        content = content
    )
}
