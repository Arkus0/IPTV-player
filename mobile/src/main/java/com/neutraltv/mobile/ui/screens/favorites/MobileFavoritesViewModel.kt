package com.neutraltv.mobile.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.core.model.ChannelDto
import com.neutraltv.mobile.data.local.CacheRepository
import com.neutraltv.mobile.data.local.CachedChannelEntity
import com.neutraltv.mobile.data.remote.TvApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<ChannelDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val userMessage: String? = null,
    val isCached: Boolean = false
)

@HiltViewModel
class MobileFavoritesViewModel @Inject constructor(
    private val tvApiClient: TvApiClient,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState

    fun loadFavorites() {
        if (!tvApiClient.isConnected) {
            loadFavoritesFromCache()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Get playlists to find the active one
            val playlistResult = tvApiClient.getPlaylists()
            val playlist = playlistResult.getOrNull()
                ?.firstOrNull { it.isActive }
                ?: playlistResult.getOrNull()?.firstOrNull()

            if (playlist == null) {
                // Could not get playlists -- try cache
                loadFavoritesFromCache(fallbackError = "No hay playlist activa")
                return@launch
            }

            tvApiClient.getFavorites(playlist.id)
                .onSuccess { favorites ->
                    _uiState.value = FavoritesUiState(favorites = favorites, isCached = false)
                }
                .onFailure { e ->
                    // API failed -- fall back to cached favorites
                    loadFavoritesFromCache(fallbackError = e.message ?: "Error al cargar favoritos")
                }
        }
    }

    private fun loadFavoritesFromCache(fallbackError: String? = null) {
        viewModelScope.launch {
            val cached = cacheRepository.getCachedFavorites()
            if (cached.isNotEmpty()) {
                val favorites = cached.map { it.toChannelDto() }
                _uiState.value = FavoritesUiState(
                    favorites = favorites,
                    isCached = true
                )
            } else {
                _uiState.value = FavoritesUiState(
                    error = fallbackError ?: "No conectado a la TV"
                )
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredFavorites(): List<ChannelDto> {
        val query = _uiState.value.searchQuery
        return if (query.isBlank()) {
            _uiState.value.favorites
        } else {
            _uiState.value.favorites.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun removeFavorite(channelId: Long) {
        viewModelScope.launch {
            tvApiClient.toggleFavorite(channelId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(userMessage = "Eliminado de favoritos")
                    loadFavorites()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(userMessage = "Error al eliminar favorito")
                }
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
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
