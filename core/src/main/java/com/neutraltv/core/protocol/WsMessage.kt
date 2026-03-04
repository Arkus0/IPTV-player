package com.neutraltv.core.protocol

data class WsMessage(
    val type: WsMessageType,
    val payload: String
)

enum class WsMessageType {
    PLAYBACK_STATE_UPDATE,
    REMOTE_COMMAND,
    TRANSFER_REQUEST,
    TRANSFER_ACK,
    FAVORITES_CHANGED,
    CHANNEL_CHANGED,
    CONNECTED,
    PING,
    PONG
}
