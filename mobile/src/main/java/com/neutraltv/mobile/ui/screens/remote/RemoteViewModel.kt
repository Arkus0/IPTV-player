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
    val tvDeviceName: String = "",
    val currentPlayback: PlaybackStateDto? = null,
    val showTransferOverlay: Boolean = false,
    val transferredState: PlaybackStateDto? = null
)

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val tvApiClient: TvApiClient,
    private val tvWebSocketClient: TvWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState

    init {
        // Observe connection state
        viewModelScope.launch {
            tvWebSocketClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    isConnected = state == ConnectionState.CONNECTED
                )
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
}
