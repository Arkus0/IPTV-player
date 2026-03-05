package com.neutraltv.player.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object AddPlaylist : Screen("add_playlist")
    data object Home : Screen("home")
    data object Channels : Screen("channels")
    data object Settings : Screen("settings")
    data object Playlists : Screen("playlists")
    data object Favorites : Screen("favorites")
    data object History : Screen("history")
    data object Epg : Screen("epg")
    data object Vod : Screen("vod")
    data object Series : Screen("series")

    data object SeriesDetail : Screen("series_detail/{seriesId}") {
        fun createRoute(seriesId: Long): String = "series_detail/$seriesId"
    }

    data object Player : Screen("player/{channelId}") {
        fun createRoute(channelId: Long): String = "player/$channelId"
    }

    data object EpisodePlayer : Screen("episode_player/{episodeId}") {
        fun createRoute(episodeId: Long): String = "episode_player/$episodeId"
    }
}
