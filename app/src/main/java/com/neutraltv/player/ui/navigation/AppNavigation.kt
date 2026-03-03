package com.neutraltv.player.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neutraltv.player.ui.screens.channels.ChannelListScreen
import com.neutraltv.player.ui.screens.epg.EpgScreen
import com.neutraltv.player.ui.screens.vod.VodScreen
import com.neutraltv.player.ui.screens.favorites.FavoritesScreen
import com.neutraltv.player.ui.screens.home.HomeScreen
import com.neutraltv.player.ui.screens.onboarding.OnboardingScreen
import com.neutraltv.player.ui.screens.player.PlayerScreen
import com.neutraltv.player.ui.screens.playlists.PlaylistSelectorScreen
import com.neutraltv.player.ui.screens.settings.SettingsScreen
import com.neutraltv.player.ui.screens.splash.SplashScreen

private const val TRANSITION_DURATION = 300
private const val FADE_DURATION = 200

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        }
    ) {
        composable(
            Screen.Splash.route,
            enterTransition = { fadeIn(tween(FADE_DURATION)) },
            exitTransition = { fadeOut(tween(FADE_DURATION)) }
        ) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Screen.Onboarding.route,
            enterTransition = { fadeIn(tween(FADE_DURATION)) },
            exitTransition = { fadeOut(tween(FADE_DURATION)) }
        ) {
            OnboardingScreen(
                onPlaylistLoaded = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AddPlaylist.route) {
            OnboardingScreen(
                onPlaylistLoaded = {
                    navController.popBackStack()
                },
                showBackButton = true,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.Home.route,
            enterTransition = { fadeIn(tween(FADE_DURATION)) },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(TRANSITION_DURATION)
                ) + fadeOut(tween(TRANSITION_DURATION))
            }
        ) {
            HomeScreen(
                onNavigateToChannels = {
                    navController.navigate(Screen.Channels.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPlaylists = {
                    navController.navigate(Screen.Playlists.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToEpg = {
                    navController.navigate(Screen.Epg.route)
                },
                onNavigateToVod = {
                    navController.navigate(Screen.Vod.route)
                },
                onNavigateToPlayer = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                }
            )
        }

        composable(Screen.Channels.route) {
            ChannelListScreen(
                onChannelSelected = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("channelId") { type = NavType.LongType }),
            enterTransition = { fadeIn(tween(FADE_DURATION)) },
            exitTransition = { fadeOut(tween(FADE_DURATION)) },
            popEnterTransition = { fadeIn(tween(FADE_DURATION)) },
            popExitTransition = { fadeOut(tween(FADE_DURATION)) }
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: return@composable
            PlayerScreen(
                channelId = channelId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPlaylistDeleted = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToPlaylists = {
                    navController.navigate(Screen.Playlists.route)
                }
            )
        }

        composable(Screen.Playlists.route) {
            PlaylistSelectorScreen(
                onBack = { navController.popBackStack() },
                onAddPlaylist = {
                    navController.navigate(Screen.AddPlaylist.route)
                },
                onPlaylistSwitched = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNoPlaylists = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onChannelSelected = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Epg.route) {
            EpgScreen(
                onChannelSelected = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Vod.route) {
            VodScreen(
                onChannelSelected = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
