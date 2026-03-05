package com.neutraltv.mobile.data.remote

import android.util.Log
import com.neutraltv.core.protocol.ApiRoutes
import com.neutraltv.core.protocol.WsMessage
import com.neutraltv.core.protocol.WsMessageSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@Singleton
class TvWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "TvWebSocket"
    }

    private val _events = MutableSharedFlow<WsMessage>(extraBufferCapacity = 50)
    val events: SharedFlow<WsMessage> = _events

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var currentHost: String? = null
    private var currentPort: Int? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(host: String, port: Int) {
        currentHost = host
        currentPort = port
        _connectionState.value = ConnectionState.CONNECTING

        val url = "ws://$host:$port${ApiRoutes.WS}"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = WsMessageSerializer.deserialize(text)
                    scope.launch { _events.emit(message) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WS message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    fun send(message: WsMessage) {
        val json = WsMessageSerializer.serialize(message)
        webSocket?.send(json)
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        currentHost = null
        currentPort = null
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun scheduleReconnect() {
        val host = currentHost ?: return
        val port = currentPort ?: return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            if (_connectionState.value == ConnectionState.DISCONNECTED) {
                Log.d(TAG, "Attempting reconnect...")
                connect(host, port)
            }
        }
    }
}
