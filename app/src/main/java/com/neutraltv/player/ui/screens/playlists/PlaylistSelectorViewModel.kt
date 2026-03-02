package com.neutraltv.player.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.PlaylistEntity
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistSelectorUiState(
    val playlists: List<PlaylistEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PlaylistSelectorViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistSelectorUiState())
    val uiState: StateFlow<PlaylistSelectorUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.getAllPlaylists().collect { playlists ->
                _uiState.value = PlaylistSelectorUiState(
                    playlists = playlists,
                    isLoading = false
                )
            }
        }
    }

    fun switchPlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.switchPlaylist(playlistId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylistById(playlistId)
        }
    }
}
