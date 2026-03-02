package com.neutraltv.player.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentChannel: ChannelEntity? = null,
    val channelList: List<ChannelEntity> = emptyList(),
    val currentIndex: Int = 0,
    val showControls: Boolean = false,
    val showChannelInfo: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    fun loadChannel(channelId: Long) {
        viewModelScope.launch {
            val channel = repository.getChannelById(channelId)
            if (channel == null) {
                _uiState.value = _uiState.value.copy(error = "Canal no encontrado")
                return@launch
            }

            val allChannels = repository.getVisibleChannelsOnce(channel.playlistId)
            val index = allChannels.indexOfFirst { it.id == channelId }.coerceAtLeast(0)

            _uiState.value = PlayerUiState(
                currentChannel = channel,
                channelList = allChannels,
                currentIndex = index,
                showChannelInfo = true
            )
        }
    }

    fun zapNext() {
        val state = _uiState.value
        if (state.channelList.isEmpty()) return

        val nextIndex = (state.currentIndex + 1) % state.channelList.size
        val nextChannel = state.channelList[nextIndex]

        _uiState.value = state.copy(
            currentChannel = nextChannel,
            currentIndex = nextIndex,
            showChannelInfo = true,
            showControls = false
        )
    }

    fun zapPrevious() {
        val state = _uiState.value
        if (state.channelList.isEmpty()) return

        val prevIndex = if (state.currentIndex > 0) {
            state.currentIndex - 1
        } else {
            state.channelList.size - 1
        }
        val prevChannel = state.channelList[prevIndex]

        _uiState.value = state.copy(
            currentChannel = prevChannel,
            currentIndex = prevIndex,
            showChannelInfo = true,
            showControls = false
        )
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(
            showControls = !_uiState.value.showControls
        )
    }

    fun hideControls() {
        _uiState.value = _uiState.value.copy(showControls = false)
    }

    fun hideChannelInfo() {
        _uiState.value = _uiState.value.copy(showChannelInfo = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
