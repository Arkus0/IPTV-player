package com.neutraltv.player.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    private var historyJob: Job? = null
    private var activePlaylistId: Long? = null

    init {
        viewModelScope.launch {
            playlistRepository.getActivePlaylist().collect { playlist ->
                if (playlist != null) {
                    activePlaylistId = playlist.id
                    loadHistory(playlist.id)
                } else {
                    activePlaylistId = null
                    _uiState.value = HistoryUiState(isLoading = false)
                }
            }
        }
    }

    private fun loadHistory(playlistId: Long) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            playlistRepository.getWatchHistory(playlistId).collect { channels ->
                _uiState.value = HistoryUiState(
                    channels = channels,
                    isLoading = false
                )
            }
        }
    }

    fun clearHistory() {
        val playlistId = activePlaylistId ?: return
        viewModelScope.launch {
            playlistRepository.clearWatchHistory(playlistId)
        }
    }
}
