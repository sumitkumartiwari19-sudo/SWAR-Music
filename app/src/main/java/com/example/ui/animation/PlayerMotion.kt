package com.example.ui.animation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

@OptIn(ExperimentalSharedTransitionApi::class)
object PlayerMotion {
    const val EXPAND_COLLAPSE_DURATION = 380
    val MotionEasing = FastOutSlowInEasing

    val PlayerBoundsTransform = BoundsTransform { _, _ ->
        tween(durationMillis = EXPAND_COLLAPSE_DURATION, easing = MotionEasing)
    }

    const val SHARED_CONTAINER_KEY = "player_container_shared_bounds"
    const val SHARED_ART_KEY = "player_art_shared_element"
}
