package com.neutraltv.player.ui.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.repository.FavoriteRepository
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelListUiState(
    val groups: List<String?> = emptyList(),
    val selectedGroup: String? = null,
    val channels: List<ChannelEntity> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val playlistId: Long? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)

@HiltViewModel
class ChannelListViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelListUiState())
    val uiState: StateFlow<ChannelListUiState> = _uiState

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getActivePlaylist().collect { playlist ->
                if (playlist != null) {
                    _uiState.value = _uiState.value.copy(playlistId = playlist.id)
                    loadGroups(playlist.id)
                    loadChannelsForGroup(playlist.id, null)
                    observeFavorites(playlist.id)
                }
            }
        }
    }

    private fun observeFavorites(playlistId: Long) {
        viewModelScope.launch {
            favoriteRepository.getFavoriteIds(playlistId).collect { ids ->
                _uiState.value = _uiState.value.copy(favoriteIds = ids.toSet())
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

    fun toggleFavorite(channelId: Long) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(channelId)
        }
    }

    fun toggleSearch() {
        val current = _uiState.value
        if (current.isSearchActive) {
            // Exit search mode and reload current group
            _uiState.value = current.copy(isSearchActive = false, searchQuery = "")
            val playlistId = current.playlistId ?: return
            if (current.selectedGroup != null) {
                loadChannelsForGroup(playlistId, current.selectedGroup)
            } else {
                viewModelScope.launch {
                    repository.getVisibleChannels(playlistId).collect { channels ->
                        _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                    }
                }
            }
        } else {
            _uiState.value = current.copy(isSearchActive = true)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        val playlistId = _uiState.value.playlistId ?: return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L) // debounce
            if (query.isBlank()) {
                // Show all channels when query is empty
                repository.getVisibleChannels(playlistId).collect { channels ->
                    _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                }
            } else {
                repository.searchChannels(playlistId, query).collect { channels ->
                    _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                }
            }
        }
    }
}
