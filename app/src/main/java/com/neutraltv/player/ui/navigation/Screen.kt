package com.neutraltv.player.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Channels : Screen("channels")
    data object Settings : Screen("settings")

    data object Player : Screen("player/{channelId}") {
        fun createRoute(channelId: Long): String = "player/$channelId"
    }
}
