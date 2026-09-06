package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Login : Screen("login", "Login")
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object Search : Screen("search", "Search", Icons.Filled.Search)
    data object Library : Screen("library", "Library", Icons.Filled.LibraryMusic)
    data object Settings : Screen("settings", "Settings")
    data object NowPlaying : Screen("now_playing", "Now Playing", Icons.Filled.PlayCircle)
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}?isImported={isImported}", "Playlist") {
        fun createRoute(playlistId: String, isImported: Boolean = false): String {
            return "playlist_detail/$playlistId?isImported=$isImported"
        }
    }

    companion object {
        val bottomNavItems = listOf(Home, Search, Library)
    }
}
