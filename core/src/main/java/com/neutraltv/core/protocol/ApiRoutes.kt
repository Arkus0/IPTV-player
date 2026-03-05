package com.neutraltv.core.protocol

object ApiRoutes {
    const val BASE = "/api/v1"
    const val DEVICE_INFO = "$BASE/device"
    const val PLAYLISTS = "$BASE/playlists"
    const val CHANNELS = "$BASE/channels"
    const val CHANNEL = "$BASE/channels/{id}"
    const val GROUPS = "$BASE/groups"
    const val FAVORITES = "$BASE/favorites"
    const val FAVORITE_TOGGLE = "$BASE/favorites/{channelId}"
    const val RECENTLY_WATCHED = "$BASE/recently-watched"
    const val PLAYBACK = "$BASE/playback"
    const val PLAYBACK_COMMAND = "$BASE/playback/command"
    const val TRANSFER = "$BASE/playback/transfer"
    const val ADD_PLAYLIST = "$BASE/playlists/add"
    const val WS = "/ws"

    const val NSD_SERVICE_TYPE = "_juanplayer._tcp."
    const val NSD_SERVICE_NAME = "JuanPlayer-TV"
    const val DEFAULT_PORT = 8642
}
