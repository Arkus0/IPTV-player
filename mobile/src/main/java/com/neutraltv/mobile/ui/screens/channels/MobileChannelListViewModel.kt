package com.neutraltv.mobile.ui.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.core.model.CommandType
import com.neutraltv.core.model.ChannelDto
import com.neutraltv.core.model.PlaylistDto
import com.neutraltv.core.model.RemoteCommand
import com.neutraltv.mobile.data.local.CacheRepository
import com.neutraltv.mobile.data.local.CachedChannelEntity
import com.neutraltv.mobile.data.remote.TvApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val error: String? = null,
    val isCached: Boolean = false
)

@HiltViewModel
class MobileChannelListViewModel @Inject constructor(
    private val tvApiClient: TvApiClient,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelListUiState())
    val uiState: StateFlow<ChannelListUiState> = _uiState

    private val _searchQuery = MutableStateFlow("")
    private val _filteredChannels = MutableStateFlow<List<ChannelDto>>(emptyList())
    val filteredChannels: StateFlow<List<ChannelDto>> = _filteredChannels

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    _filteredChannels.value = if (query.isBlank()) {
                        _uiState.value.channels
                    } else {
                        _uiState.value.channels.filter {
                            it.name.contains(query, ignoreCase = true) ||
                                (it.groupTitle?.contains(query, ignoreCase = true) == true)
                        }
                    }
                }
        }
    }

    init {
        setupSearchDebounce()
        loadPlaylists()
    }

    private fun loadPlaylists() {
        if (!tvApiClient.isConnected) {
            // TV not connected -- try loading from cache
            loadFromCache()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            tvApiClient.getPlaylists().fold(
                onSuccess = { playlists ->
                    val active = playlists.firstOrNull { it.isActive } ?: playlists.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        playlists = playlists,
                        selectedPlaylist = active,
                        isLoading = false,
                        isCached = false
                    )
                    active?.let { loadChannels(it.id) }
                },
                onFailure = { e ->
                    // API failed -- fall back to cache
                    loadFromCache(fallbackError = e.message)
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
                        isLoading = false,
                        isCached = false,
                        error = null
                    )
                    _filteredChannels.value = if (_searchQuery.value.isBlank()) {
                        channels
                    } else {
                        channels.filter {
                            it.name.contains(_searchQuery.value, ignoreCase = true) ||
                                (it.groupTitle?.contains(_searchQuery.value, ignoreCase = true) == true)
                        }
                    }
                    // Cache channels in background after successful fetch
                    if (group == null) {
                        cacheRepository.cacheChannels(channels)
                    }
                },
                onFailure = { e ->
                    // API failed -- fall back to cache
                    loadFromCache(fallbackError = e.message)
                }
            )
        }
    }

    private fun loadFromCache(fallbackError: String? = null) {
        viewModelScope.launch {
            val cached = cacheRepository.getCachedChannels()
            if (cached.isNotEmpty()) {
                val channels = cached.map { it.toChannelDto() }
                val groups = cacheRepository.getCachedGroups()
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    groups = groups,
                    isLoading = false,
                    isCached = true,
                    error = null
                )
                _filteredChannels.value = if (_searchQuery.value.isBlank()) {
                    channels
                } else {
                    channels.filter {
                        it.name.contains(_searchQuery.value, ignoreCase = true) ||
                            (it.groupTitle?.contains(_searchQuery.value, ignoreCase = true) == true)
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = fallbackError ?: "No hay conexion y no hay datos en cache"
                )
            }
        }
    }

    fun selectGroup(group: String?) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
        val playlist = _uiState.value.selectedPlaylist
        if (playlist != null && tvApiClient.isConnected) {
            loadChannels(playlist.id)
        } else {
            // Filter from cache by group
            viewModelScope.launch {
                val cached = if (group != null) {
                    cacheRepository.getCachedByGroup(group)
                } else {
                    cacheRepository.getCachedChannels()
                }
                val channels = cached.map { it.toChannelDto() }
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    isCached = true
                )
                _filteredChannels.value = if (_searchQuery.value.isBlank()) {
                    channels
                } else {
                    channels.filter {
                        it.name.contains(_searchQuery.value, ignoreCase = true) ||
                            (it.groupTitle?.contains(_searchQuery.value, ignoreCase = true) == true)
                    }
                }
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchQuery.value = query
    }

    fun refresh() {
        loadPlaylists()
    }

    fun playOnTv(channelId: Long) {
        viewModelScope.launch {
            tvApiClient.sendCommand(RemoteCommand(type = CommandType.PLAY_CHANNEL, channelId = channelId))
        }
    }

    fun toggleFavorite(channelId: Long) {
        viewModelScope.launch {
            tvApiClient.toggleFavorite(channelId)
                .onSuccess {
                    _uiState.value.selectedPlaylist?.let { loadChannels(it.id) }
                }
        }
    }
}

/** Convert a cached entity back into a ChannelDto for use by the UI. */
private fun CachedChannelEntity.toChannelDto(): ChannelDto = ChannelDto(
    id = id,
    playlistId = 0,
    name = name,
    streamUrl = streamUrl,
    logoUrl = logoUrl,
    groupTitle = groupTitle,
    isFavorite = isFavorite
)
