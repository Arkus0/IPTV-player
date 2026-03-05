package com.neutraltv.core.protocol

import com.google.gson.Gson
import com.neutraltv.core.model.PlaybackStateDto
import com.neutraltv.core.model.RemoteCommand
import com.neutraltv.core.model.TransferRequest
import com.neutraltv.core.model.SyncFavorites

object WsMessageSerializer {

    @PublishedApi
    internal val gson = Gson()

    fun serialize(message: WsMessage): String = gson.toJson(message)

    fun deserialize(json: String): WsMessage = gson.fromJson(json, WsMessage::class.java)

    fun playbackUpdate(state: PlaybackStateDto): WsMessage = WsMessage(
        type = WsMessageType.PLAYBACK_STATE_UPDATE,
        payload = gson.toJson(state)
    )

    fun remoteCommand(command: RemoteCommand): WsMessage = WsMessage(
        type = WsMessageType.REMOTE_COMMAND,
        payload = gson.toJson(command)
    )

    fun transferRequest(request: TransferRequest): WsMessage = WsMessage(
        type = WsMessageType.TRANSFER_REQUEST,
        payload = gson.toJson(request)
    )

    fun transferAck(state: PlaybackStateDto): WsMessage = WsMessage(
        type = WsMessageType.TRANSFER_ACK,
        payload = gson.toJson(state)
    )

    fun favoritesChanged(favorites: SyncFavorites): WsMessage = WsMessage(
        type = WsMessageType.FAVORITES_CHANGED,
        payload = gson.toJson(favorites)
    )

    fun connected(deviceName: String): WsMessage = WsMessage(
        type = WsMessageType.CONNECTED,
        payload = deviceName
    )

    fun ping(): WsMessage = WsMessage(
        type = WsMessageType.PING,
        payload = System.currentTimeMillis().toString()
    )

    fun pong(): WsMessage = WsMessage(
        type = WsMessageType.PONG,
        payload = System.currentTimeMillis().toString()
    )

    inline fun <reified T> parsePayload(message: WsMessage): T =
        gson.fromJson(message.payload, T::class.java)
}
