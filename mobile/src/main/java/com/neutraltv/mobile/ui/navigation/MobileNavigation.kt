package com.neutraltv.mobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neutraltv.mobile.ui.screens.channels.MobileChannelListScreen
import com.neutraltv.mobile.ui.screens.connect.ConnectScreen
import com.neutraltv.mobile.ui.screens.favorites.MobileFavoritesScreen
import com.neutraltv.mobile.ui.screens.player.MobilePlayerScreen
import com.neutraltv.mobile.ui.screens.remote.RemoteScreen
import com.neutraltv.mobile.ui.screens.settings.MobileSettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(MobileScreen.Remote.route, "Control", Icons.Default.Tv),
    BottomNavItem(MobileScreen.Channels.route, "Canales", Icons.Default.LiveTv),
    BottomNavItem(MobileScreen.Favorites.route, "Favoritos", Icons.Default.Favorite),
    BottomNavItem(MobileScreen.Settings.route, "Ajustes", Icons.Default.Settings)
)

@Composable
fun MobileNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route?.let { route ->
        bottomNavItems.any { it.route == route }
    } ?: false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MobileScreen.Connect.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MobileScreen.Connect.route) {
                ConnectScreen(
                    onConnected = {
                        navController.navigate(MobileScreen.Remote.route) {
                            popUpTo(MobileScreen.Connect.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(MobileScreen.Remote.route) {
                RemoteScreen(
                    onNavigateToPlayer = { channelId, streamUrl, channelName ->
                        val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                        val encodedName = URLEncoder.encode(channelName, "UTF-8")
                        navController.navigate(
                            MobileScreen.Player.createRoute(channelId, encodedUrl, encodedName)
                        )
                    }
                )
            }

            composable(MobileScreen.Channels.route) {
                MobileChannelListScreen(
                    onChannelClick = { channelId, streamUrl, channelName ->
                        val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                        val encodedName = URLEncoder.encode(channelName, "UTF-8")
                        navController.navigate(
                            MobileScreen.Player.createRoute(channelId, encodedUrl, encodedName)
                        )
                    }
                )
            }

            composable(MobileScreen.Favorites.route) {
                MobileFavoritesScreen(
                    onChannelClick = { channelId, streamUrl, channelName ->
                        val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                        val encodedName = URLEncoder.encode(channelName, "UTF-8")
                        navController.navigate(
                            MobileScreen.Player.createRoute(channelId, encodedUrl, encodedName)
                        )
                    }
                )
            }

            composable(MobileScreen.Settings.route) {
                MobileSettingsScreen(
                    onDisconnected = {
                        navController.navigate(MobileScreen.Connect.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = MobileScreen.Player.route,
                arguments = listOf(
                    navArgument("channelId") { type = NavType.LongType },
                    navArgument("streamUrl") { type = NavType.StringType },
                    navArgument("channelName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getLong("channelId") ?: 0L
                val streamUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString("streamUrl") ?: "", "UTF-8"
                )
                val channelName = URLDecoder.decode(
                    backStackEntry.arguments?.getString("channelName") ?: "", "UTF-8"
                )
                MobilePlayerScreen(
                    channelId = channelId,
                    streamUrl = streamUrl,
                    channelName = channelName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
