package com.example.ui.screens.playlist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import com.example.ui.theme.SwarMusicTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaylistDetailCollapsingToolbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPlaylistDetailScreenRendersWithCollapsingComponents() {
        var backClicked = false

        composeTestRule.setContent {
            SwarMusicTheme {
                PlaylistDetailScreen(
                    playlistId = "default_my_fav_songs",
                    isImported = false,
                    onNavigateBack = { backClicked = true }
                )
            }
        }

        // Verify the pinned top bar back button is rendered
        composeTestRule.onNodeWithTag("playlist_back_button").assertExists()

        // Verify the unified LazyColumn is present
        composeTestRule.onNodeWithTag("playlist_tracks_list").assertExists()

        // Verify play all and shuffle buttons are rendered
        composeTestRule.onNodeWithTag("play_all_button").assertExists()
        composeTestRule.onNodeWithTag("shuffle_playlist_button").assertExists()
        composeTestRule.onNodeWithTag("download_all_button").assertExists()
    }

    @Test
    fun testPlaylistDetailScreenScrollsDownAndUp() {
        composeTestRule.setContent {
            SwarMusicTheme {
                PlaylistDetailScreen(
                    playlistId = "default_my_fav_songs",
                    isImported = false,
                    onNavigateBack = {}
                )
            }
        }

        // Scroll to action row (index 1)
        composeTestRule.onNodeWithTag("playlist_tracks_list")
            .performScrollToIndex(1)

        // Pinned top bar stays displayed when scrolled
        composeTestRule.onNodeWithTag("playlist_back_button").assertExists()
        composeTestRule.onNodeWithTag("playlist_delete_button").assertExists()

        // Scroll back up to top (index 0)
        composeTestRule.onNodeWithTag("playlist_tracks_list")
            .performScrollToIndex(0)

        // Header actions are existing
        composeTestRule.onNodeWithTag("play_all_button").assertExists()
    }
}
