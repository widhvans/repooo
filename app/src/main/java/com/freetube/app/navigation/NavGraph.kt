package com.freetube.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.freetube.app.ui.channel.ChannelScreen
import com.freetube.app.ui.home.HomeScreen
import com.freetube.app.ui.library.LibraryScreen
import com.freetube.app.ui.library.DownloadsScreen
import com.freetube.app.ui.library.HistoryScreen
import com.freetube.app.ui.library.WatchLaterScreen
import com.freetube.app.ui.player.PlayerScreen
import com.freetube.app.ui.search.SearchScreen
import com.freetube.app.ui.settings.DebugLogsScreen
import com.freetube.app.ui.settings.SettingsScreen
import com.freetube.app.ui.shorts.ShortsScreen
import com.freetube.app.ui.subscriptions.SubscriptionsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { 30 },
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { 30 },
                animationSpec = tween(300)
            )
        }
    ) {
        // Home
        composable(Screen.Home.route) {
            HomeScreen(
                onVideoClick = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Channel.createRoute(channelId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onDebugClick = {
                    navController.navigate(Screen.DebugLogs.route)
                }
            )
        }
        
        // Shorts
        composable(Screen.Shorts.route) {
            ShortsScreen(
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Channel.createRoute(channelId))
                }
            )
        }
        
        // Subscriptions
        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen(
                onVideoClick = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Channel.createRoute(channelId))
                }
            )
        }
        
        // Library
        composable(Screen.Library.route) {
            LibraryScreen(
                onVideoClick = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.route)
                },
                onDownloadsClick = {
                    navController.navigate(Screen.Downloads.route)
                },
                onWatchLaterClick = {
                    navController.navigate(Screen.WatchLater.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        // Search
        composable(Screen.Search.route) {
            SearchScreen(
                onVideoClick = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Channel.createRoute(channelId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Video Player
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
            PlayerScreen(
                videoId = videoId,
                onBackClick = {
                    navController.popBackStack()
                },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Channel.createRoute(channelId))
                },
                onVideoClick = { newVideoId ->
                    navController.navigate(Screen.Player.createRoute(newVideoId))
                }
            )
        }
        
        // Channel
        composable(
            route = Screen.Channel.route,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            ChannelScreen(
                channelId = channelId,
                onBackClick = {
                    navController.popBackStack()
                },
                onVideoClick = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                }
            )
        }
        
        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // History
        composable(Screen.History.route) {
            HistoryScreen(
                onVideoClick = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Downloads
        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onVideoClick = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Watch Later
        composable(Screen.WatchLater.route) {
            WatchLaterScreen(
                onVideoClick = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Debug Logs
        composable(Screen.DebugLogs.route) {
            DebugLogsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
