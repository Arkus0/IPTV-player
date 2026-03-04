package com.neutraltv.mobile.ui.navigation

sealed class MobileScreen(val route: String) {
    data object Connect : MobileScreen("connect")
    data object Remote : MobileScreen("remote")
    data object Channels : MobileScreen("channels")
    data object Favorites : MobileScreen("favorites")
    data object Settings : MobileScreen("settings")

    data object Player : MobileScreen("player/{channelId}/{streamUrl}/{channelName}") {
        fun createRoute(channelId: Long, streamUrl: String, channelName: String): String =
            "player/$channelId/${java.net.URLEncoder.encode(streamUrl, "UTF-8")}/${java.net.URLEncoder.encode(channelName, "UTF-8")}"
    }
}
