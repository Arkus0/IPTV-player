package com.neutraltv.mobile.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.core.model.CommandType
import com.neutraltv.core.model.PlaybackStateDto
import com.neutraltv.core.model.RemoteCommand
import com.neutraltv.core.model.TransferDirection
import com.neutraltv.core.model.TransferRequest
import com.neutraltv.mobile.data.remote.ConnectionState
import com.neutraltv.mobile.data.remote.TvApiClient
import com.neutraltv.mobile.data.remote.TvWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MobilePlayerUiState(
    val channelId: Long = 0,
    val channelName: String = "",
    val streamUrl: String = "",
    val isPlaying: Boolean = true,
    val position: Long = 0,
    val duration: Long = 0,
    val isTvConnected: Boolean = false,
    val showTransferOverlay: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MobilePlayerViewModel @Inject constructor(
    private val tvApiClient: TvApiClient,
    private val tvWebSocketClient: TvWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MobilePlayerUiState())
    val uiState: StateFlow<MobilePlayerUiState> = _uiState

    init {
        viewModelScope.launch {
            tvWebSocketClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    isTvConnected = state == ConnectionState.CONNECTED
                )
            }
        }
    }

    fun loadChannel(channelId: Long, streamUrl: String, channelName: String) {
        _uiState.value = _uiState.value.copy(
            channelId = channelId,
            channelName = channelName,
            streamUrl = streamUrl,
            isPlaying = true
        )
    }

    fun updateProgress(position: Long, duration: Long) {
        _uiState.value = _uiState.value.copy(position = position, duration = duration)
    }

    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun transferToTv() {
        viewModelScope.launch {
            val state = _uiState.value
            val playbackState = PlaybackStateDto(
                channelId = state.channelId,
                channelName = state.channelName,
                streamUrl = state.streamUrl,
                positionMs = state.position,
                durationMs = state.duration,
                isPlaying = true
            )

            _uiState.value = _uiState.value.copy(
                showTransferOverlay = true,
                isPlaying = false // Pause local playback
            )

            val request = TransferRequest(
                direction = TransferDirection.TO_TV,
                playbackState = playbackState
            )
            tvApiClient.requestTransfer(request)
        }
    }

    fun seekForward() {
        viewModelScope.launch {
            tvApiClient.sendCommand(RemoteCommand(type = CommandType.SEEK_FORWARD))
        }
    }

    fun seekBackward() {
        viewModelScope.launch {
            tvApiClient.sendCommand(RemoteCommand(type = CommandType.SEEK_BACKWARD))
        }
    }

    fun volumeUp() {
        viewModelScope.launch {
            tvApiClient.sendCommand(RemoteCommand(type = CommandType.VOLUME_UP))
        }
    }

    fun volumeDown() {
        viewModelScope.launch {
            tvApiClient.sendCommand(RemoteCommand(type = CommandType.VOLUME_DOWN))
        }
    }

    fun dismissTransferOverlay() {
        _uiState.value = _uiState.value.copy(showTransferOverlay = false)
    }
}
