package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.data.preferences.ThemeMode
import com.example.ui.animation.PlayerMotion
import com.example.ui.ads.AdBannerView
import com.example.ui.ads.AdUnit
import com.example.ui.components.NeumorphicInsetSurface
import com.example.ui.components.NeumorphicMiniPlayer
import com.example.ui.components.NeumorphicSurface
import com.example.ui.playback.PlaybackViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.nowplaying.NowPlayingScreen
import com.example.ui.screens.playlist.PlaylistDetailScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.pip.PipVideoScreen
import com.example.ui.theme.NeumorphicTheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SwarNavGraph(
    navController: NavHostController,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(
        factory = PlaybackViewModel.provideFactory(LocalContext.current)
    ),
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isNowPlayingScreen = currentRoute == Screen.NowPlaying.route
    val isSettingsScreen = currentRoute == Screen.Settings.route

    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val isLiked by playbackViewModel.isCurrentTrackLiked.collectAsStateWithLifecycle()

    val startDestination = Screen.Home.route

    if (isInPipMode) {
        PipVideoScreen(
            exoPlayer = playbackViewModel.exoPlayer,
            playbackState = playbackState,
            modifier = modifier.fillMaxSize()
        )
    } else {
        SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
            val sharedTransitionScope = this

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = NeumorphicTheme.colors.background,
                bottomBar = {
                    AnimatedVisibility(
                        visible = !isNowPlayingScreen && !isSettingsScreen,
                        enter = fadeIn(tween(durationMillis = 200, delayMillis = 100)),
                        exit = fadeOut(tween(durationMillis = 150))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Persistent 320x50 Banner on Home and Library (anchored above mini-player / nav)
                            val showAnchoredBanner = (currentRoute == Screen.Home.route || currentRoute == Screen.Library.route)
                            if (showAnchoredBanner) {
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
                            }

                            NeumorphicMiniPlayer(
                                playbackState = playbackState,
                                isLiked = isLiked,
                                onPlayPause = { playbackViewModel.togglePlayPause() },
                                onSkipNext = { playbackViewModel.skipNext() },
                                onToggleLike = { playbackViewModel.toggleLikeCurrentTrack() },
                                onClick = {
                                    navController.navigate(Screen.NowPlaying.route) {
                                        launchSingleTop = true
                                    }
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = this@AnimatedVisibility
                            )

                            SwarBottomNavigationBar(
                                currentRoute = currentRoute,
                                onNavigate = { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isNowPlayingScreen) PaddingValues(0.dp) else innerPadding)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            currentThemeMode = currentThemeMode,
                            onThemeModeChange = onThemeModeChange,
                            viewModel = homeViewModel,
                            playbackViewModel = playbackViewModel,
                            onNavigateToSearch = {
                                navController.navigate(Screen.Search.route)
                            },
                            onNavigateToNowPlaying = {
                                navController.navigate(Screen.NowPlaying.route)
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                    }

                    composable(Screen.Search.route) {
                        SearchScreen(
                            homeViewModel = homeViewModel,
                            playbackViewModel = playbackViewModel
                        )
                    }

                    composable(Screen.Library.route) {
                        LibraryScreen(
                            homeViewModel = homeViewModel,
                            playbackViewModel = playbackViewModel,
                            onNavigateToPlaylist = { playlistId, isImported ->
                                navController.navigate(Screen.PlaylistDetail.createRoute(playlistId, isImported))
                            },
                            onNavigateToNowPlaying = {
                                navController.navigate(Screen.NowPlaying.route)
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.PlaylistDetail.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("playlistId") {
                                type = androidx.navigation.NavType.StringType
                            },
                            androidx.navigation.navArgument("isImported") {
                                type = androidx.navigation.NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                        val isImported = backStackEntry.arguments?.getBoolean("isImported") ?: false
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            isImported = isImported,
                            onNavigateBack = { navController.popBackStack() },
                            homeViewModel = homeViewModel,
                            playbackViewModel = playbackViewModel
                        )
                    }

                    composable(
                        route = Screen.NowPlaying.route,
                        enterTransition = {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = PlayerMotion.EXPAND_COLLAPSE_DURATION,
                                    easing = PlayerMotion.MotionEasing
                                )
                            )
                        },
                        exitTransition = {
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = PlayerMotion.EXPAND_COLLAPSE_DURATION,
                                    easing = PlayerMotion.MotionEasing
                                )
                            )
                        },
                        popEnterTransition = {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = PlayerMotion.EXPAND_COLLAPSE_DURATION,
                                    easing = PlayerMotion.MotionEasing
                                )
                            )
                        },
                        popExitTransition = {
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = PlayerMotion.EXPAND_COLLAPSE_DURATION,
                                    easing = PlayerMotion.MotionEasing
                                )
                            )
                        }
                    ) {
                        NowPlayingScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onEnterPip = onEnterPip,
                            homeViewModel = homeViewModel,
                            playbackViewModel = playbackViewModel,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }
    }
}

/**
 * Neumorphic Bottom Navigation Bar.
 * Active tab is highlighted with an inset pill background and accent-colored icon.
 */
@Composable
fun SwarBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NeumorphicSurface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        cornerRadius = 0.dp,
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val isSelected = currentRoute == screen.route

                if (isSelected) {
                    // Active Tab: Inset pill background + Accent colored icon & text
                    NeumorphicInsetSurface(
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("nav_item_${screen.route}"),
                        cornerRadius = 22.dp,
                        depth = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            screen.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title,
                                    tint = NeumorphicTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = screen.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTheme.colors.accent
                            )
                        }
                    }
                } else {
                    // Inactive Tab: Raised or flat clickable area
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(screen) }
                            )
                            .testTag("nav_item_${screen.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            screen.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title,
                                    tint = NeumorphicTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = screen.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NeumorphicTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
