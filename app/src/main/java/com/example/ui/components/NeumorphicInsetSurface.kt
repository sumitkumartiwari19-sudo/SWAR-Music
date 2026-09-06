package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeumorphicTheme

/**
 * Inset/Pressed-in/Concave Neumorphic Surface.
 * Ideal for search bars, sliders, progress tracks, and pressed-in toggle states.
 * Renders subtle inner shadows with top-left dark depression and bottom-right light reflection.
 */
@Composable
fun NeumorphicInsetSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isCircle: Boolean = false,
    depth: Dp = 4.dp,
    backgroundColor: Color = NeumorphicTheme.colors.background,
    lightShadowColor: Color = NeumorphicTheme.colors.raisedShadowLight,
    darkShadowColor: Color = NeumorphicTheme.colors.raisedShadowDark,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val depthPx = with(density) { depth.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val blurRadius = (depthPx * 1.5f).coerceAtLeast(1f)

    val shape = if (isCircle) CircleShape else RoundedCornerShape(cornerRadius)

    val innerShadowModifier = Modifier
        .clip(shape)
        .background(backgroundColor)
        .drawWithContent {
            val width = size.width
            val height = size.height

            drawContent()

            if (width <= 0f || height <= 0f) return@drawWithContent

            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas

                val clipPath = android.graphics.Path().apply {
                    if (isCircle) {
                        val radius = (width.coerceAtMost(height)) / 2f
                        addCircle(width / 2f, height / 2f, radius, android.graphics.Path.Direction.CW)
                    } else {
                        addRoundRect(
                            0f, 0f, width, height,
                            cornerRadiusPx, cornerRadiusPx,
                            android.graphics.Path.Direction.CW
                        )
                    }
                }

                // 1. Top-Left Inset Dark Shadow
                val darkPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = darkShadowColor.toArgb()
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = depthPx * 2.5f
                    maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                }

                nativeCanvas.save()
                nativeCanvas.clipPath(clipPath)
                nativeCanvas.translate(-depthPx * 0.5f, -depthPx * 0.5f)
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

                // 2. Bottom-Right Inset Light Shadow
                val lightPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = lightShadowColor.toArgb()
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = depthPx * 2.5f
                    maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                }

                nativeCanvas.save()
                nativeCanvas.clipPath(clipPath)
                nativeCanvas.translate(depthPx * 0.5f, depthPx * 0.5f)
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
            }
        }

    Box(
        modifier = modifier.then(innerShadowModifier),
        contentAlignment = contentAlignment,
        content = content
    )
}
