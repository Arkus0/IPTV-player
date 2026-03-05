package com.neutraltv.mobile.ui.screens.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.core.model.CommandType
import com.neutraltv.core.model.PlaybackStateDto
import com.neutraltv.core.model.RemoteCommand
import com.neutraltv.core.model.TransferDirection
import com.neutraltv.core.model.TransferRequest
import com.neutraltv.core.protocol.WsMessageSerializer
import com.neutraltv.core.protocol.WsMessageType
import com.neutraltv.mobile.data.remote.ConnectionState
import com.neutraltv.mobile.data.remote.TvApiClient
import com.neutraltv.mobile.data.remote.TvWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemoteUiState(
    val isConnected: Boolean = false,
    val isReconnecting: Boolean = false,
    val reconnectAttempt: Int = 0,
    val tvDeviceName: String = "",
    val currentPlayback: PlaybackStateDto? = null,
    val showTransferOverlay: Boolean = false,
    val transferredState: PlaybackStateDto? = null,
    val isFavorite: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val tvApiClient: TvApiClient,
    private val tvWebSocketClient: TvWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState

    init {
        // Observe connection state and reconnect attempts
        viewModelScope.launch {
            tvWebSocketClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    isConnected = state == ConnectionState.CONNECTED,
                    isReconnecting = state == ConnectionState.RECONNECTING
                )
            }
        }

        viewModelScope.launch {
            tvWebSocketClient.reconnectAttempt.collect { attempt ->
                _uiState.value = _uiState.value.copy(reconnectAttempt = attempt)
            }
        }

        // Observe WebSocket events for playback updates
        viewModelScope.launch {
            tvWebSocketClient.events.collect { message ->
                when (message.type) {
                    WsMessageType.PLAYBACK_STATE_UPDATE -> {
                        val state = WsMessageSerializer.parsePayload<PlaybackStateDto>(message)
                        _uiState.value = _uiState.value.copy(currentPlayback = state)
                    }
                    WsMessageType.CONNECTED -> {
                        _uiState.value = _uiState.value.copy(tvDeviceName = message.payload)
                        // Fetch initial playback state
                        fetchPlaybackState()
                    }
                    WsMessageType.TRANSFER_ACK -> {
                        val state = WsMessageSerializer.parsePayload<PlaybackStateDto>(message)
                        _uiState.value = _uiState.value.copy(
                            showTransferOverlay = true,
                            transferredState = state
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    private fun fetchPlaybackState() {
        viewModelScope.launch {
            tvApiClient.getPlaybackState().onSuccess { state ->
                _uiState.value = _uiState.value.copy(currentPlayback = state)
            }
        }
    }

    fun sendCommand(type: CommandType, channelId: Long? = null) {
        viewModelScope.launch {
            val command = RemoteCommand(type = type, channelId = channelId)
            tvApiClient.sendCommand(command)
        }
    }

    fun toggleFavorite() {
        val channelId = _uiState.value.currentPlayback?.channelId ?: return
        viewModelScope.launch {
            tvApiClient.toggleFavorite(channelId)
                .onSuccess { result ->
                    val isFav = result["isFavorite"] ?: false
                    _uiState.value = _uiState.value.copy(
                        isFavorite = isFav,
                        userMessage = if (isFav) "Agregado a favoritos" else "Eliminado de favoritos"
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        userMessage = "Error al actualizar favorito"
                    )
                }
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    fun requestTransferToMobile() {
        viewModelScope.launch {
            val request = TransferRequest(direction = TransferDirection.TO_MOBILE)
            val result = tvApiClient.requestTransfer(request)
            result.onSuccess { state ->
                if (state != null) {
                    _uiState.value = _uiState.value.copy(
                        showTransferOverlay = true,
                        transferredState = state
                    )
                }
            }
        }
    }

    fun dismissTransferOverlay() {
        _uiState.value = _uiState.value.copy(
            showTransferOverlay = false
        )
    }

    fun getTransferredState(): PlaybackStateDto? = _uiState.value.transferredState

    fun reconnect() {
        tvWebSocketClient.reconnect()
    }
}
