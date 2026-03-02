package com.neutraltv.player.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.repository.FavoriteRepository
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState

    init {
        viewModelScope.launch {
            playlistRepository.getActivePlaylist().collect { playlist ->
                if (playlist != null) {
                    loadFavorites(playlist.id)
                } else {
                    _uiState.value = FavoritesUiState(isLoading = false)
                }
            }
        }
    }

    private fun loadFavorites(playlistId: Long) {
        viewModelScope.launch {
            favoriteRepository.getFavoriteChannels(playlistId).collect { channels ->
                _uiState.value = FavoritesUiState(
                    channels = channels,
                    isLoading = false
                )
            }
        }
    }

    fun removeFavorite(channelId: Long) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(channelId)
        }
    }
}
