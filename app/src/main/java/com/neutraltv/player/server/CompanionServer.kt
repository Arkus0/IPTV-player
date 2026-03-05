package com.neutraltv.player.server

import android.util.Log
import com.google.gson.Gson
import com.neutraltv.core.model.DeviceInfo
import com.neutraltv.core.model.DeviceType
import com.neutraltv.core.model.RemoteCommand
import com.neutraltv.core.model.TransferDirection
import com.neutraltv.core.model.TransferRequest
import com.neutraltv.core.protocol.ApiRoutes
import com.neutraltv.core.protocol.WsMessage
import com.neutraltv.core.protocol.WsMessageSerializer
import com.neutraltv.core.protocol.WsMessageType
import com.neutraltv.player.data.local.dao.ChannelDao
import com.neutraltv.player.data.local.dao.FavoriteDao
import com.neutraltv.player.data.local.dao.PlaylistDao
import com.neutraltv.player.data.repository.FavoriteRepository
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionServer @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao,
    private val favoriteRepository: FavoriteRepository,
    private val playlistRepository: com.neutraltv.player.data.repository.PlaylistRepository,
    private val playbackBridge: PlaybackBridge
) {

    companion object {
        private const val TAG = "CompanionServer"
    }

    private val gson = Gson()
    private var server: ApplicationEngine? = null
    private val wsConnections = Collections.synchronizedSet(
        mutableSetOf<io.ktor.server.websocket.WebSocketServerSession>()
    )

    val port: Int = ApiRoutes.DEFAULT_PORT

    fun start() {
        if (server != null) return

        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) { gson() }
            install(WebSockets)

            routing {
                // Device info
                get(ApiRoutes.DEVICE_INFO) {
                    val info = DeviceInfo(
                        deviceName = android.os.Build.MODEL,
                        deviceType = DeviceType.TV
                    )
                    call.respond(info)
                }

                // Playlists
                get(ApiRoutes.PLAYLISTS) {
                    val playlists = playlistDao.getAllOnce()
                    call.respond(playlists.map { it.toDto() })
                }

                // Add playlist from URL (mobile paste feature)
                post(ApiRoutes.ADD_PLAYLIST) {
                    try {
                        val body = call.receive<Map<String, String>>()
                        val url = body["url"]
                        val name = body["name"] ?: "Playlist"

                        if (url.isNullOrBlank()) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "url is required"))
                            return@post
                        }

                        val result = playlistRepository.loadPlaylistFromUrl(name, url)
                        result.onSuccess { playlist ->
                            call.respond(HttpStatusCode.Created, playlist.toDto())
                        }.onFailure { e ->
                            call.respond(
                                HttpStatusCode.UnprocessableEntity,
                                mapOf("error" to (e.message ?: "Failed to load playlist"))
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding playlist", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to (e.message ?: "Internal error"))
                        )
                    }
                }

                // Channels
                get(ApiRoutes.CHANNELS) {
                    val playlistId = call.request.queryParameters["playlistId"]?.toLongOrNull()
                    val group = call.request.queryParameters["group"]
                    val type = call.request.queryParameters["type"] ?: "live"

                    if (playlistId == null) {
                        call.respond(HttpStatusCode.BadRequest, "playlistId required")
                        return@get
                    }

                    val channels = if (type == "vod") {
                        if (group != null) {
                            channelDao.getVodByGroup(playlistId, group)
                        } else {
                            channelDao.getVodChannels(playlistId).first()
                        }
                    } else {
                        if (group != null) {
                            channelDao.getChannelsByGroup(playlistId, group).first()
                        } else {
                            channelDao.getVisibleChannelsOnce(playlistId)
                        }
                    }

                    val favoriteIds = favoriteDao.getFavoriteIds(playlistId).first().toSet()
                    call.respond(channels.map { it.toDto(isFavorite = it.id in favoriteIds) })
                }

                // Groups
                get(ApiRoutes.GROUPS) {
                    val playlistId = call.request.queryParameters["playlistId"]?.toLongOrNull()
                    if (playlistId == null) {
                        call.respond(HttpStatusCode.BadRequest, "playlistId required")
                        return@get
                    }
                    val groups = channelDao.getGroups(playlistId).first()
                    call.respond(groups.filterNotNull())
                }

                // Favorites
                get(ApiRoutes.FAVORITES) {
                    val playlistId = call.request.queryParameters["playlistId"]?.toLongOrNull()
                    if (playlistId == null) {
                        call.respond(HttpStatusCode.BadRequest, "playlistId required")
                        return@get
                    }
                    val favorites = favoriteDao.getFavoriteChannels(playlistId).first()
                    call.respond(favorites.map { it.toDto(isFavorite = true) })
                }

                // Toggle favorite
                post("${ApiRoutes.FAVORITES}/{channelId}") {
                    val channelId = call.parameters["channelId"]?.toLongOrNull()
                    if (channelId == null) {
                        call.respond(HttpStatusCode.BadRequest, "channelId required")
                        return@post
                    }
                    favoriteRepository.toggleFavorite(channelId)
                    val isFav = favoriteDao.isFavoriteOnce(channelId)
                    call.respond(mapOf("isFavorite" to isFav))

                    // Notify WebSocket clients
                    broadcastFavoritesChanged()
                }

                // Recently watched
                get(ApiRoutes.RECENTLY_WATCHED) {
                    val playlistId = call.request.queryParameters["playlistId"]?.toLongOrNull()
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                    if (playlistId == null) {
                        call.respond(HttpStatusCode.BadRequest, "playlistId required")
                        return@get
                    }
                    val recent = channelDao.getRecentlyWatched(playlistId, limit).first()
                    call.respond(recent.map { it.toHistoryEntry() })
                }

                // Current playback state
                get(ApiRoutes.PLAYBACK) {
                    val state = playbackBridge.currentPlaybackState.value
                    if (state != null) {
                        call.respond(state)
                    } else {
                        call.respond(HttpStatusCode.NoContent, "")
                    }
                }

                // Remote command
                post(ApiRoutes.PLAYBACK_COMMAND) {
                    val command = call.receive<RemoteCommand>()
                    playbackBridge.handleCommand(command)
                    call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
                }

                // Transfer request
                post(ApiRoutes.TRANSFER) {
                    val request = call.receive<TransferRequest>()
                    if (request.direction == TransferDirection.TO_MOBILE) {
                        // Client wants the current TV playback state
                        val state = playbackBridge.currentPlaybackState.value
                        if (state != null) {
                            playbackBridge.handleTransfer(request)
                            call.respond(state)
                        } else {
                            call.respond(HttpStatusCode.NoContent, "No active playback")
                        }
                    } else {
                        // Client sends playback to TV
                        playbackBridge.handleTransfer(request)
                        call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
                    }
                }

                // WebSocket
                webSocket(ApiRoutes.WS) {
                    wsConnections.add(this)
                    playbackBridge.updateConnectedClients(wsConnections.size)
                    Log.d(TAG, "WebSocket connected. Total: ${wsConnections.size}")

                    // Send current state on connect
                    send(Frame.Text(WsMessageSerializer.serialize(
                        WsMessageSerializer.connected(android.os.Build.MODEL)
                    )))

                    // Forward playback state updates to this client
                    val stateJob = launch {
                        playbackBridge.currentPlaybackState.collect { state ->
                            state?.let {
                                try {
                                    send(Frame.Text(WsMessageSerializer.serialize(
                                        WsMessageSerializer.playbackUpdate(it)
                                    )))
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error sending state update", e)
                                }
                            }
                        }
                    }

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                handleWsMessage(text)
                            }
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        Log.d(TAG, "WebSocket closed")
                    } catch (e: Exception) {
                        Log.e(TAG, "WebSocket error", e)
                    } finally {
                        stateJob.cancel()
                        wsConnections.remove(this)
                        playbackBridge.updateConnectedClients(wsConnections.size)
                        Log.d(TAG, "WebSocket disconnected. Total: ${wsConnections.size}")
                    }
                }
            }
        }.start(wait = false)

        Log.d(TAG, "Server started on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        wsConnections.clear()
        playbackBridge.updateConnectedClients(0)
        Log.d(TAG, "Server stopped")
    }

    fun isRunning(): Boolean = server != null

    private suspend fun handleWsMessage(json: String) {
        try {
            val message = WsMessageSerializer.deserialize(json)
            when (message.type) {
                WsMessageType.REMOTE_COMMAND -> {
                    val command = WsMessageSerializer.parsePayload<RemoteCommand>(message)
                    playbackBridge.handleCommand(command)
                }
                WsMessageType.TRANSFER_REQUEST -> {
                    val request = WsMessageSerializer.parsePayload<TransferRequest>(message)
                    playbackBridge.handleTransfer(request)

                    // Send ACK with current state
                    val state = playbackBridge.currentPlaybackState.value
                    if (state != null) {
                        broadcastMessage(WsMessageSerializer.transferAck(state))
                    }
                }
                WsMessageType.PING -> {
                    broadcastMessage(WsMessageSerializer.pong())
                }
                else -> Log.d(TAG, "Unhandled WS message type: ${message.type}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling WS message", e)
        }
    }

    private suspend fun broadcastMessage(message: WsMessage) {
        val text = WsMessageSerializer.serialize(message)
        wsConnections.forEach { session: io.ktor.server.websocket.WebSocketServerSession ->
            try {
                session.send(Frame.Text(text))
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting", e)
            }
        }
    }

    private suspend fun broadcastFavoritesChanged() {
        val activePlaylist = playlistDao.getActivePlaylistOnce() ?: return
        val favorites = favoriteDao.getFavoriteChannels(activePlaylist.id).first()
        val favDtos = favorites.map {
            com.neutraltv.core.model.FavoriteDto(
                channelId = it.id,
                channelName = it.name,
                addedAt = System.currentTimeMillis()
            )
        }
        broadcastMessage(WsMessageSerializer.favoritesChanged(
            com.neutraltv.core.model.SyncFavorites(favorites = favDtos)
        ))
    }
}
