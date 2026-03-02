package com.neutraltv.player.ui.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelListUiState(
    val groups: List<String?> = emptyList(),
    val selectedGroup: String? = null,
    val channels: List<ChannelEntity> = emptyList(),
    val isLoading: Boolean = true,
    val playlistId: Long? = null
)

@HiltViewModel
class ChannelListViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelListUiState())
    val uiState: StateFlow<ChannelListUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.getActivePlaylist().collect { playlist ->
                if (playlist != null) {
                    _uiState.value = _uiState.value.copy(playlistId = playlist.id)
                    loadGroups(playlist.id)
                    loadChannelsForGroup(playlist.id, null)
                }
            }
        }
    }

    private fun loadGroups(playlistId: Long) {
        viewModelScope.launch {
            repository.getGroups(playlistId).collect { groups ->
                _uiState.value = _uiState.value.copy(groups = groups)
            }
        }
    }

    private fun loadChannelsForGroup(playlistId: Long, group: String?) {
        viewModelScope.launch {
            repository.getChannelsByGroup(playlistId, group).collect { channels ->
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    isLoading = false
                )
            }
        }
    }

    fun selectGroup(group: String?) {
        val playlistId = _uiState.value.playlistId ?: return
        _uiState.value = _uiState.value.copy(selectedGroup = group, isLoading = true)
        loadChannelsForGroup(playlistId, group)
    }

    fun selectAllChannels() {
        val playlistId = _uiState.value.playlistId ?: return
        _uiState.value = _uiState.value.copy(selectedGroup = null, isLoading = true)
        viewModelScope.launch {
            repository.getVisibleChannels(playlistId).collect { channels ->
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    selectedGroup = null,
                    isLoading = false
                )
            }
        }
    }
}
