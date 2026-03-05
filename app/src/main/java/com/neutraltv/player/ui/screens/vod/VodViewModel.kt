package com.neutraltv.player.ui.screens.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VodUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val groups: List<String?> = emptyList(),
    val selectedGroup: String? = null,
    val isAllSelected: Boolean = true,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true,
    val playlistId: Long = 0
)

@HiltViewModel
class VodViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VodUiState())
    val uiState: StateFlow<VodUiState> = _uiState

    private var searchJob: Job? = null
    private var vodChannelsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getActivePlaylist().collect { playlist ->
                if (playlist != null) {
                    _uiState.value = _uiState.value.copy(playlistId = playlist.id)
                    loadGroups(playlist.id)
                    loadAllVod(playlist.id)
                }
            }
        }
    }

    private fun loadGroups(playlistId: Long) {
        viewModelScope.launch {
            val groups = repository.getVodGroups(playlistId)
            _uiState.value = _uiState.value.copy(groups = groups)
        }
    }

    private fun loadAllVod(playlistId: Long) {
        vodChannelsJob?.cancel()
        vodChannelsJob = viewModelScope.launch {
            repository.getVodChannels(playlistId).collect { channels ->
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    isLoading = false
                )
            }
        }
    }

    fun selectAll() {
        _uiState.value = _uiState.value.copy(
            isAllSelected = true,
            selectedGroup = null,
            isLoading = true
        )
        vodChannelsJob?.cancel()
        vodChannelsJob = viewModelScope.launch {
            repository.getVodChannels(_uiState.value.playlistId).collect { channels ->
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    isLoading = false
                )
            }
        }
    }

    fun selectGroup(group: String?) {
        _uiState.value = _uiState.value.copy(
            isAllSelected = false,
            selectedGroup = group,
            isLoading = true
        )
        viewModelScope.launch {
            val channels = repository.getVodByGroup(_uiState.value.playlistId, group)
            _uiState.value = _uiState.value.copy(
                channels = channels,
                isLoading = false
            )
        }
    }

    fun toggleSearch() {
        val newSearchActive = !_uiState.value.isSearchActive
        _uiState.value = _uiState.value.copy(
            isSearchActive = newSearchActive,
            searchQuery = if (!newSearchActive) "" else _uiState.value.searchQuery
        )
        if (!newSearchActive) {
            selectAll()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (query.isBlank()) {
                selectAll()
            } else {
                val results = repository.searchVodChannels(_uiState.value.playlistId, query)
                _uiState.value = _uiState.value.copy(channels = results)
            }
        }
    }
}
