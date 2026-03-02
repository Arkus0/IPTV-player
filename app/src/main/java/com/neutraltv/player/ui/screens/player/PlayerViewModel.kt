package com.neutraltv.player.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.player.StreamRetryManager
import com.neutraltv.player.data.repository.EpgRepository
import com.neutraltv.player.data.repository.FavoriteRepository
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StreamErrorType { NETWORK, SOURCE, UNKNOWN }

data class PlayerUiState(
    val currentChannel: ChannelEntity? = null,
    val channelList: List<ChannelEntity> = emptyList(),
    val currentIndex: Int = 0,
    val showControls: Boolean = false,
    val showChannelInfo: Boolean = false,
    val isFavorite: Boolean = false,
    val error: String? = null,
    val isRetrying: Boolean = false,
    val retryAttempt: Int = 0,
    val maxRetries: Int = 3,
    val errorType: StreamErrorType? = null,
    val showErrorOverlay: Boolean = false,
    val currentProgramTitle: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val epgRepository: EpgRepository,
    private val retryManager: StreamRetryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    // Event to trigger player retry from the UI
    private val _retryEvent = MutableSharedFlow<Unit>()
    val retryEvent: SharedFlow<Unit> = _retryEvent

    fun loadChannel(channelId: Long) {
        viewModelScope.launch {
            val channel = repository.getChannelById(channelId)
            if (channel == null) {
                _uiState.value = _uiState.value.copy(error = "Canal no encontrado")
                return@launch
            }

            val allChannels = repository.getVisibleChannelsOnce(channel.playlistId)
            val index = allChannels.indexOfFirst { it.id == channelId }.coerceAtLeast(0)

            retryManager.reset()
            _uiState.value = PlayerUiState(
                currentChannel = channel,
                channelList = allChannels,
                currentIndex = index,
                showChannelInfo = true
            )

            repository.markChannelWatched(channelId)
            observeFavoriteStatus(channelId)
            observeCurrentProgram(channel)
        }
    }

    private fun observeCurrentProgram(channel: ChannelEntity) {
        val epgChannelId = channel.epgChannelId ?: return
        viewModelScope.launch {
            epgRepository.getCurrentProgram(epgChannelId).collect { program ->
                _uiState.value = _uiState.value.copy(
                    currentProgramTitle = program?.title
                )
            }
        }
    }

    private fun observeFavoriteStatus(channelId: Long) {
        viewModelScope.launch {
            favoriteRepository.isFavorite(channelId).collect { isFav ->
                _uiState.value = _uiState.value.copy(isFavorite = isFav)
            }
        }
    }

    fun toggleFavorite() {
        val channelId = _uiState.value.currentChannel?.id ?: return
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(channelId)
        }
    }

    fun onPlayerError(errorCode: Int) {
        val errorType = classifyError(errorCode)
        _uiState.value = _uiState.value.copy(errorType = errorType)

        if (retryManager.canRetry()) {
            val attempt = retryManager.currentAttempt + 1
            _uiState.value = _uiState.value.copy(
                isRetrying = true,
                retryAttempt = attempt,
                maxRetries = retryManager.maxRetries,
                showErrorOverlay = true
            )
            viewModelScope.launch {
                delay(retryManager.getNextDelay())
                if (_uiState.value.isRetrying) {
                    _retryEvent.emit(Unit)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isRetrying = false,
                showErrorOverlay = true,
                error = when (errorType) {
                    StreamErrorType.NETWORK -> "Error de conexión"
                    StreamErrorType.SOURCE -> "No se puede reproducir esta fuente"
                    StreamErrorType.UNKNOWN -> "Error desconocido"
                }
            )
        }
    }

    private fun classifyError(errorCode: Int): StreamErrorType {
        return when {
            errorCode in 2000..2999 -> StreamErrorType.NETWORK
            errorCode in 3000..3999 -> StreamErrorType.SOURCE
            else -> StreamErrorType.UNKNOWN
        }
    }

    fun retryManually() {
        retryManager.reset()
        _uiState.value = _uiState.value.copy(
            isRetrying = true,
            retryAttempt = 1,
            showErrorOverlay = true,
            error = null
        )
        viewModelScope.launch {
            retryManager.getNextDelay() // consume first slot
            _retryEvent.emit(Unit)
        }
    }

    fun skipToNextChannel() {
        dismissError()
        zapNext()
    }

    fun dismissError() {
        retryManager.reset()
        _uiState.value = _uiState.value.copy(
            isRetrying = false,
            showErrorOverlay = false,
            error = null,
            errorType = null,
            retryAttempt = 0
        )
    }

    fun zapNext() {
        val state = _uiState.value
        if (state.channelList.isEmpty()) return

        val nextIndex = (state.currentIndex + 1) % state.channelList.size
        val nextChannel = state.channelList[nextIndex]

        retryManager.reset()
        _uiState.value = state.copy(
            currentChannel = nextChannel,
            currentIndex = nextIndex,
            showChannelInfo = true,
            showControls = false,
            isRetrying = false,
            showErrorOverlay = false,
            error = null,
            errorType = null,
            retryAttempt = 0
        )

        viewModelScope.launch {
            repository.markChannelWatched(nextChannel.id)
            observeFavoriteStatus(nextChannel.id)
            observeCurrentProgram(nextChannel)
        }
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

        retryManager.reset()
        _uiState.value = state.copy(
            currentChannel = prevChannel,
            currentIndex = prevIndex,
            showChannelInfo = true,
            showControls = false,
            isRetrying = false,
            showErrorOverlay = false,
            error = null,
            errorType = null,
            retryAttempt = 0
        )

        viewModelScope.launch {
            repository.markChannelWatched(prevChannel.id)
            observeFavoriteStatus(prevChannel.id)
            observeCurrentProgram(prevChannel)
        }
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
