package com.freetube.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for the app
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    // Bottom navigation screens
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Shorts : Screen(
        route = "shorts",
        title = "Shorts",
        selectedIcon = Icons.Filled.Movie,
        unselectedIcon = Icons.Outlined.Movie
    )
    
    data object Subscriptions : Screen(
        route = "subscriptions",
        title = "Subscriptions",
        selectedIcon = Icons.Filled.Subscriptions,
        unselectedIcon = Icons.Outlined.Subscriptions
    )
    
    data object Library : Screen(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary
    )
    
    // Other screens
    data object Search : Screen(
        route = "search",
        title = "Search"
    )
    
    data object Player : Screen(
        route = "player/{videoId}",
        title = "Player"
    ) {
        fun createRoute(videoId: String) = "player/$videoId"
    }
    
    data object Channel : Screen(
        route = "channel/{channelId}",
        title = "Channel"
    ) {
        fun createRoute(channelId: String) = "channel/$channelId"
    }
    
    data object Playlist : Screen(
        route = "playlist/{playlistId}",
        title = "Playlist"
    ) {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    
    data object Settings : Screen(
        route = "settings",
        title = "Settings"
    )
    
    data object History : Screen(
        route = "history",
        title = "History"
    )
    
    data object Downloads : Screen(
        route = "downloads",
        title = "Downloads"
    )
    
    data object WatchLater : Screen(
        route = "watch_later",
        title = "Watch Later"
    )
    
    data object DebugLogs : Screen(
        route = "debug_logs",
        title = "Debug Logs"
    )
    
    data object Login : Screen(
        route = "login",
        title = "Login"
    )
    
    companion object {
        val bottomNavItems = listOf(Home, Shorts, Subscriptions, Library)
    }
}
