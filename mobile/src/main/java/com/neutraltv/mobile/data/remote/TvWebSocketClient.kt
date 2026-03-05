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
import kotlin.math.min

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

@Singleton
class TvWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "TvWebSocket"
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }

    private val _events = MutableSharedFlow<WsMessage>(extraBufferCapacity = 50)
    val events: SharedFlow<WsMessage> = _events

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _reconnectAttempt = MutableStateFlow(0)
    val reconnectAttempt: StateFlow<Int> = _reconnectAttempt

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var currentHost: String? = null
    private var currentPort: Int? = null
    private var intentionalDisconnect = false
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(host: String, port: Int) {
        currentHost = host
        currentPort = port
        intentionalDisconnect = false
        _reconnectAttempt.value = 0
        connectInternal(host, port)
    }

    private fun connectInternal(host: String, port: Int) {
        // Close any existing socket before reconnecting
        webSocket?.close(1000, null)
        webSocket = null

        if (_connectionState.value != ConnectionState.RECONNECTING) {
            _connectionState.value = ConnectionState.CONNECTING
        }

        val url = "ws://$host:$port${ApiRoutes.WS}"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionState.value = ConnectionState.CONNECTED
                _reconnectAttempt.value = 0
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
                handleConnectionLost()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                handleConnectionLost()
            }
        })
    }

    fun send(message: WsMessage) {
        val json = WsMessageSerializer.serialize(message)
        webSocket?.send(json)
    }

    fun disconnect() {
        intentionalDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        currentHost = null
        currentPort = null
        _reconnectAttempt.value = 0
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Manually trigger a reconnection attempt, resetting the attempt counter.
     * Useful after max retries have been exhausted and the user wants to try again.
     */
    fun reconnect() {
        val host = currentHost
        val port = currentPort
        if (host != null && port != null) {
            intentionalDisconnect = false
            _reconnectAttempt.value = 0
            _connectionState.value = ConnectionState.RECONNECTING
            scheduleReconnect()
        }
    }

    private fun handleConnectionLost() {
        if (intentionalDisconnect) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        val host = currentHost
        val port = currentPort
        if (host == null || port == null) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        val currentAttempt = _reconnectAttempt.value
        if (currentAttempt >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnection attempts ($MAX_RECONNECT_ATTEMPTS) reached, giving up")
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        _connectionState.value = ConnectionState.RECONNECTING
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        val host = currentHost ?: return
        val port = currentPort ?: return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val attempt = _reconnectAttempt.value
            val backoffMs = min(INITIAL_BACKOFF_MS * (1L shl attempt), MAX_BACKOFF_MS)
            val nextAttempt = attempt + 1
            _reconnectAttempt.value = nextAttempt

            Log.d(TAG, "Reconnect attempt $nextAttempt/$MAX_RECONNECT_ATTEMPTS in ${backoffMs}ms")
            delay(backoffMs)

            if (!intentionalDisconnect && _connectionState.value == ConnectionState.RECONNECTING) {
                connectInternal(host, port)
            }
        }
    }
}
