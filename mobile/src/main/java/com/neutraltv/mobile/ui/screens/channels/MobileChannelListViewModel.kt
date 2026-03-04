package com.neutraltv.mobile.ui.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.core.model.ChannelDto
import com.neutraltv.core.model.PlaylistDto
import com.neutraltv.mobile.data.remote.TvApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelListUiState(
    val playlists: List<PlaylistDto> = emptyList(),
    val selectedPlaylist: PlaylistDto? = null,
    val channels: List<ChannelDto> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null
)

@HiltViewModel
class MobileChannelListViewModel @Inject constructor(
    private val tvApiClient: TvApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelListUiState())
    val uiState: StateFlow<ChannelListUiState> = _uiState

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        if (!tvApiClient.isConnected) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            tvApiClient.getPlaylists().fold(
                onSuccess = { playlists ->
                    val active = playlists.firstOrNull { it.isActive } ?: playlists.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        playlists = playlists,
                        selectedPlaylist = active,
                        isLoading = false
                    )
                    active?.let { loadChannels(it.id) }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    private fun loadChannels(playlistId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load groups
            tvApiClient.getGroups(playlistId).onSuccess { groups ->
                _uiState.value = _uiState.value.copy(groups = groups)
            }

            // Load channels
            val group = _uiState.value.selectedGroup
            tvApiClient.getChannels(playlistId, group).fold(
                onSuccess = { channels ->
                    _uiState.value = _uiState.value.copy(
                        channels = channels,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun selectGroup(group: String?) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
        _uiState.value.selectedPlaylist?.let { loadChannels(it.id) }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun refresh() {
        loadPlaylists()
    }

    fun getFilteredChannels(): List<ChannelDto> {
        val query = _uiState.value.searchQuery
        return if (query.isBlank()) {
            _uiState.value.channels
        } else {
            _uiState.value.channels.filter {
                it.name.contains(query, ignoreCase = true) ||
                    (it.groupTitle?.contains(query, ignoreCase = true) == true)
            }
        }
    }
}
