package com.neutraltv.core.model

data class RemoteCommand(
    val type: CommandType,
    val channelId: Long? = null
)

enum class CommandType {
    CHANNEL_UP,
    CHANNEL_DOWN,
    TOGGLE_PLAY_PAUSE,
    TOGGLE_FAVORITE,
    PLAY_CHANNEL,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    BACK,
    HOME,
    OK,
    SEEK_FORWARD,
    SEEK_BACKWARD
}
