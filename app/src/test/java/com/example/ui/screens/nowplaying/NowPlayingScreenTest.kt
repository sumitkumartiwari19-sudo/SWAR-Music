package com.example.ui.screens.nowplaying

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.theme.SwarMusicTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NowPlayingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNowPlayingTopBarAndFourPillActionRowRendered() {
        var backClicked = false

        composeTestRule.setContent {
            SwarMusicTheme {
                NowPlayingScreen(
                    onNavigateBack = { backClicked = true }
                )
            }
        }

        // Verify Top Bar elements are present
        composeTestRule.onNodeWithTag("now_playing_back_button").assertExists()
        composeTestRule.onNodeWithTag("audio_video_toggle_pill").assertExists()
        composeTestRule.onNodeWithTag("now_playing_overflow_button").assertExists()

        // Verify the 4-pill action row elements
        composeTestRule.onNodeWithTag("now_playing_action_row").assertExists()
        composeTestRule.onNodeWithTag("now_playing_like_button").assertExists()
        composeTestRule.onNodeWithTag("now_playing_lyrics_button").assertExists()
        composeTestRule.onNodeWithTag("now_playing_save_button").assertExists()
        composeTestRule.onNodeWithTag("now_playing_download_button").assertExists()
    }
}
