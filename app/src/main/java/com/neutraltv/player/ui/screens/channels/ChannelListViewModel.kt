package com.neutraltv.player.ui.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.preferences.PreferencesRepository
import com.neutraltv.player.data.repository.EpgRepository
import com.neutraltv.player.data.repository.FavoriteRepository
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelListUiState(
    val groups: List<String?> = emptyList(),
    val selectedGroup: String? = null,
    val channels: List<ChannelEntity> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val currentPrograms: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val playlistId: Long? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showHiddenChannels: Boolean = false
)

@HiltViewModel
class ChannelListViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val epgRepository: EpgRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelListUiState())
    val uiState: StateFlow<ChannelListUiState> = _uiState

    private var searchJob: Job? = null
    private var epgObserveJob: Job? = null
    private var channelsJob: Job? = null
    private var groupsJob: Job? = null
    private var favoritesJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getActivePlaylist().collect { playlist ->
                if (playlist != null) {
                    _uiState.value = _uiState.value.copy(playlistId = playlist.id)
                    loadGroups(playlist.id)
                    loadChannelsForGroup(playlist.id, null)
                    observeFavorites(playlist.id)
                    val epgUrl = playlist.epgUrl
                        ?: preferencesRepository.getUserPreferences().first().customEpgUrl.takeIf { it.isNotBlank() }
                    if (epgUrl != null) {
                        fetchEpgAndRefresh(playlist.id, epgUrl)
                    }
                }
            }
        }
    }

    private fun observeFavorites(playlistId: Long) {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            favoriteRepository.getFavoriteIds(playlistId).collect { ids ->
                _uiState.value = _uiState.value.copy(favoriteIds = ids.toSet())
            }
        }
    }

    private fun loadGroups(playlistId: Long) {
        groupsJob?.cancel()
        groupsJob = viewModelScope.launch {
            repository.getGroups(playlistId).collect { groups ->
                _uiState.value = _uiState.value.copy(groups = groups)
            }
        }
    }

    private fun loadChannelsForGroup(playlistId: Long, group: String?) {
        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            val showAll = _uiState.value.showHiddenChannels
            val flow = if (showAll) {
                repository.getAllChannelsByGroup(playlistId, group)
            } else {
                repository.getChannelsByGroup(playlistId, group)
            }
            flow.collect { channels ->
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    isLoading = false
                )
                observeCurrentPrograms(channels)
            }
        }
    }

    private fun observeCurrentPrograms(channels: List<ChannelEntity>) {
        epgObserveJob?.cancel()
        val epgIds = channels.mapNotNull { it.epgChannelId }
        if (epgIds.isEmpty()) return
        epgObserveJob = viewModelScope.launch {
            epgRepository.getCurrentProgramsMapFlow(epgIds).collect { programs ->
                _uiState.value = _uiState.value.copy(currentPrograms = programs)
            }
        }
    }

    private fun fetchEpgAndRefresh(playlistId: Long, epgUrl: String) {
        viewModelScope.launch {
            epgRepository.loadEpg(playlistId, epgUrl)
            // After EPG fetch, the reactive Flow in observeCurrentPrograms will auto-update
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
        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            val showAll = _uiState.value.showHiddenChannels
            val flow = if (showAll) {
                repository.getAllChannelsFlow(playlistId)
            } else {
                repository.getVisibleChannels(playlistId)
            }
            flow.collect { channels ->
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    selectedGroup = null,
                    isLoading = false
                )
                observeCurrentPrograms(channels)
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
                channelsJob?.cancel()
                channelsJob = viewModelScope.launch {
                    val showAll = _uiState.value.showHiddenChannels
                    val flow = if (showAll) {
                        repository.getAllChannelsFlow(playlistId)
                    } else {
                        repository.getVisibleChannels(playlistId)
                    }
                    flow.collect { channels ->
                        _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                        observeCurrentPrograms(channels)
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
            val showAll = _uiState.value.showHiddenChannels
            if (query.isBlank()) {
                // Show all channels when query is empty
                val flow = if (showAll) {
                    repository.getAllChannelsFlow(playlistId)
                } else {
                    repository.getVisibleChannels(playlistId)
                }
                flow.collect { channels ->
                    _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                    observeCurrentPrograms(channels)
                }
            } else {
                val flow = if (showAll) {
                    repository.searchAllChannels(playlistId, query)
                } else {
                    repository.searchChannels(playlistId, query)
                }
                flow.collect { channels ->
                    _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                    observeCurrentPrograms(channels)
                }
            }
        }
    }

    fun toggleShowHidden() {
        val current = _uiState.value
        val newShowHidden = !current.showHiddenChannels
        _uiState.value = current.copy(showHiddenChannels = newShowHidden, isLoading = true)
        reloadCurrentChannels()
    }

    fun toggleChannelHidden(channelId: Long) {
        viewModelScope.launch {
            repository.toggleChannelHidden(channelId)
        }
    }

    private fun reloadCurrentChannels() {
        val state = _uiState.value
        val playlistId = state.playlistId ?: return
        if (state.isSearchActive && state.searchQuery.isNotBlank()) {
            onSearchQueryChanged(state.searchQuery)
        } else if (state.selectedGroup != null) {
            loadChannelsForGroup(playlistId, state.selectedGroup)
        } else {
            channelsJob?.cancel()
            channelsJob = viewModelScope.launch {
                val flow = if (state.showHiddenChannels) {
                    repository.getAllChannelsFlow(playlistId)
                } else {
                    repository.getVisibleChannels(playlistId)
                }
                flow.collect { channels ->
                    _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                    observeCurrentPrograms(channels)
                }
            }
        }
    }
}
