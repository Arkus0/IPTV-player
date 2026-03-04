package com.neutraltv.player.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.PlaylistEntity
import com.neutraltv.player.data.repository.FavoriteRepository
import com.neutraltv.player.data.repository.PlaylistRepository
import com.neutraltv.player.data.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val playlist: PlaylistEntity? = null,
    val recentChannels: List<ChannelEntity> = emptyList(),
    val favoriteChannels: List<ChannelEntity> = emptyList(),
    val vodCount: Int = 0,
    val seriesCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.getActivePlaylist().collect { playlist ->
                _uiState.value = _uiState.value.copy(
                    playlist = playlist,
                    isLoading = false
                )
                if (playlist != null) {
                    loadRecentChannels(playlist.id)
                    loadFavoriteChannels(playlist.id)
                    loadVodCount(playlist.id)
                    loadSeriesCount(playlist.id)
                }
            }
        }
    }

    private fun loadRecentChannels(playlistId: Long) {
        viewModelScope.launch {
            repository.getRecentlyWatched(playlistId, 5).collect { channels ->
                _uiState.value = _uiState.value.copy(recentChannels = channels)
            }
        }
    }

    private fun loadFavoriteChannels(playlistId: Long) {
        viewModelScope.launch {
            favoriteRepository.getFavoriteChannels(playlistId).collect { channels ->
                _uiState.value = _uiState.value.copy(favoriteChannels = channels)
            }
        }
    }

    private fun loadVodCount(playlistId: Long) {
        viewModelScope.launch {
            val count = repository.getVodCount(playlistId)
            _uiState.value = _uiState.value.copy(vodCount = count)
        }
    }

    private fun loadSeriesCount(playlistId: Long) {
        viewModelScope.launch {
            val count = xtreamRepository.getSeriesCount(playlistId)
            _uiState.value = _uiState.value.copy(seriesCount = count)
        }
    }
}
