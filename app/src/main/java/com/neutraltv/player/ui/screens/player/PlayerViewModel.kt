package com.neutraltv.player.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.player.StreamRetryManager
import com.neutraltv.player.data.preferences.PreferencesRepository
import com.neutraltv.player.data.repository.EpgRepository
import com.neutraltv.player.data.repository.FavoriteRepository
import com.neutraltv.player.data.repository.PlaylistRepository
import com.neutraltv.player.data.repository.XtreamRepository
import com.neutraltv.player.server.PlaybackBridge
import com.neutraltv.core.model.CommandType
import com.neutraltv.core.model.PlaybackStateDto
import com.neutraltv.core.model.TransferDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    val currentProgramTitle: String? = null,
    // Timeshift
    val isLive: Boolean = true,
    val isTimeshifted: Boolean = false,
    val liveOffsetMs: Long = 0,
    val isPlaying: Boolean = true,
    // VOD
    val isVod: Boolean = false,
    val vodProgress: Long = 0,
    val vodDuration: Long = 0,
    // Episode
    val isEpisode: Boolean = false,
    val episodeId: Long = 0,
    // Companion
    val companionConnected: Boolean = false,
    val showTransferOverlay: Boolean = false,
    val transferDirection: TransferDirection? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val epgRepository: EpgRepository,
    private val retryManager: StreamRetryManager,
    private val preferencesRepository: PreferencesRepository,
    private val xtreamRepository: XtreamRepository,
    private val playbackBridge: PlaybackBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    // Event to trigger player retry from the UI
    private val _retryEvent = MutableSharedFlow<Unit>()
    val retryEvent: SharedFlow<Unit> = _retryEvent

    // Events for commands that need to be handled at the Screen/Activity level
    private val _seekEvent = MutableSharedFlow<Long>(extraBufferCapacity = 5)
    val seekEvent: SharedFlow<Long> = _seekEvent

    private val _volumeEvent = MutableSharedFlow<Int>(extraBufferCapacity = 5)
    val volumeEvent: SharedFlow<Int> = _volumeEvent

    init {
        // Observe remote commands from companion mobile app
        viewModelScope.launch {
            playbackBridge.remoteCommands.collect { command ->
                when (command.type) {
                    CommandType.CHANNEL_UP -> zapNext()
                    CommandType.CHANNEL_DOWN -> zapPrevious()
                    CommandType.TOGGLE_PLAY_PAUSE -> togglePlayPause()
                    CommandType.TOGGLE_FAVORITE -> toggleFavorite()
                    CommandType.PLAY_CHANNEL -> command.channelId?.let { loadChannel(it) }
                    CommandType.SEEK_FORWARD -> { _seekEvent.tryEmit(10_000L) }
                    CommandType.SEEK_BACKWARD -> { _seekEvent.tryEmit(-10_000L) }
                    CommandType.BACK -> hideControls()
                    CommandType.OK -> toggleControls()
                    CommandType.VOLUME_UP -> { _volumeEvent.tryEmit(1) }
                    CommandType.VOLUME_DOWN -> { _volumeEvent.tryEmit(-1) }
                    CommandType.MUTE -> { _volumeEvent.tryEmit(0) }
                    else -> { /* HOME and other unhandled commands */ }
                }
            }
        }

        // Observe transfer requests from companion mobile app
        viewModelScope.launch {
            playbackBridge.transferRequests.collect { request ->
                when (request.direction) {
                    TransferDirection.TO_TV -> {
                        // Mobile sends playback to TV
                        request.playbackState?.let { state ->
                            _uiState.value = _uiState.value.copy(
                                showTransferOverlay = true,
                                transferDirection = TransferDirection.TO_TV
                            )
                            // Load the channel at the specified position
                            loadChannel(state.channelId)
                        }
                    }
                    TransferDirection.TO_MOBILE -> {
                        // TV sends playback to mobile — show overlay, pause TV playback
                        _uiState.value = _uiState.value.copy(
                            showTransferOverlay = true,
                            transferDirection = TransferDirection.TO_MOBILE,
                            isPlaying = false
                        )
                    }
                }
            }
        }

        // Publish playback state to companion bridge (only when playback-relevant fields change)
        viewModelScope.launch {
            uiState
                .map { state ->
                    state.currentChannel?.let { channel ->
                        PlaybackStateDto(
                            channelId = channel.id,
                            channelName = channel.name,
                            streamUrl = channel.streamUrl,
                            logoUrl = channel.logoUrl,
                            positionMs = state.vodProgress,
                            durationMs = state.vodDuration,
                            isVod = state.isVod,
                            isEpisode = state.isEpisode,
                            episodeId = state.episodeId,
                            isPlaying = state.isPlaying
                        )
                    }
                }
                .distinctUntilChanged()
                .collect { playbackState ->
                    if (playbackState != null) {
                        playbackBridge.updatePlaybackState(playbackState)
                    }
                }
        }

        // Track companion connection status
        viewModelScope.launch {
            playbackBridge.connectedClients.collect { count ->
                _uiState.value = _uiState.value.copy(companionConnected = count > 0)
            }
        }
    }

    fun loadChannel(channelId: Long) {
        viewModelScope.launch {
            val channel = repository.getChannelById(channelId)
            if (channel == null) {
                _uiState.value = _uiState.value.copy(error = "Canal no encontrado")
                return@launch
            }

            val isVod = channel.channelType == "vod"
            val allChannels = repository.getVisibleChannelsOnce(channel.playlistId)
            val index = allChannels.indexOfFirst { it.id == channelId }.coerceAtLeast(0)

            retryManager.reset()
            _uiState.value = PlayerUiState(
                currentChannel = channel,
                channelList = allChannels,
                currentIndex = index,
                showChannelInfo = true,
                isVod = isVod,
                isLive = !isVod,
                vodProgress = if (isVod) channel.vodProgress else 0
            )

            repository.markChannelWatched(channelId)
            observeFavoriteStatus(channelId)
            if (!isVod) {
                observeCurrentProgram(channel)
                fetchEpgIfNeeded(channel.playlistId)
            }
        }
    }

    private fun fetchEpgIfNeeded(playlistId: Long) {
        viewModelScope.launch {
            val playlist = repository.getActivePlaylistOnce() ?: return@launch
            val epgUrl = playlist.epgUrl
                ?: preferencesRepository.getUserPreferences().first().customEpgUrl.takeIf { it.isNotBlank() }
                ?: return@launch
            // Check if we already have EPG data
            val channel = _uiState.value.currentChannel ?: return@launch
            val epgChannelId = channel.epgChannelId ?: return@launch
            val existing = epgRepository.getCurrentProgramOnce(epgChannelId)
            if (existing == null) {
                val result = epgRepository.loadEpg(playlistId, epgUrl)
                result.onFailure { e ->
                    android.util.Log.w("PlayerViewModel", "EPG fetch failed: ${e.message}")
                }
            }
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

    // Timeshift methods
    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(
            isPlaying = !_uiState.value.isPlaying
        )
    }

    fun updatePlayingState(isPlaying: Boolean) {
        val wasPlaying = _uiState.value.isPlaying
        _uiState.value = _uiState.value.copy(
            isPlaying = isPlaying,
            isTimeshifted = if (!_uiState.value.isVod && !isPlaying) true else _uiState.value.isTimeshifted
        )
    }

    fun updateLiveOffset(offsetMs: Long) {
        val isAtLive = offsetMs < 3000 // within 3s of live edge
        _uiState.value = _uiState.value.copy(
            liveOffsetMs = offsetMs,
            isTimeshifted = !isAtLive && !_uiState.value.isVod
        )
    }

    fun seekToLive() {
        _uiState.value = _uiState.value.copy(
            isTimeshifted = false,
            liveOffsetMs = 0
        )
    }

    // VOD methods
    fun updateVodProgress(position: Long, duration: Long) {
        _uiState.value = _uiState.value.copy(
            vodProgress = position,
            vodDuration = duration
        )
    }

    fun saveVodProgress() {
        val state = _uiState.value
        if (state.isEpisode) {
            if (state.episodeId > 0 && state.vodProgress > 0) {
                viewModelScope.launch {
                    xtreamRepository.updateEpisodeProgress(state.episodeId, state.vodProgress)
                }
            }
            return
        }
        val channelId = state.currentChannel?.id ?: return
        if (!state.isVod || state.vodProgress <= 0) return
        viewModelScope.launch {
            repository.saveVodProgress(channelId, state.vodProgress)
        }
    }

    fun dismissTransferOverlay() {
        _uiState.value = _uiState.value.copy(
            showTransferOverlay = false,
            transferDirection = null
        )
    }

    fun loadEpisode(episodeId: Long) {
        viewModelScope.launch {
            val episode = xtreamRepository.getEpisodeById(episodeId)
            if (episode == null) {
                _uiState.value = _uiState.value.copy(error = "Episodio no encontrado")
                return@launch
            }

            // Create a synthetic ChannelEntity for the player UI
            val syntheticChannel = ChannelEntity(
                id = 0,
                playlistId = episode.playlistId,
                name = episode.title,
                streamUrl = episode.streamUrl,
                channelType = "vod",
                position = 0
            )

            retryManager.reset()
            _uiState.value = PlayerUiState(
                currentChannel = syntheticChannel,
                showChannelInfo = true,
                isVod = true,
                isLive = false,
                isEpisode = true,
                episodeId = episodeId,
                vodProgress = episode.progress
            )
        }
    }
}
