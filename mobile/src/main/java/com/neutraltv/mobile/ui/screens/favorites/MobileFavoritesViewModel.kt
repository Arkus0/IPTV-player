package com.neutraltv.mobile.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.core.model.FavoriteDto
import com.neutraltv.mobile.data.remote.TvApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<FavoriteDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class MobileFavoritesViewModel @Inject constructor(
    private val tvApiClient: TvApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState

    fun loadFavorites() {
        if (!tvApiClient.isConnected) {
            _uiState.value = FavoritesUiState(error = "No conectado a la TV")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            tvApiClient.getFavorites()
                .onSuccess { favorites ->
                    _uiState.value = FavoritesUiState(favorites = favorites)
                }
                .onFailure { e ->
                    _uiState.value = FavoritesUiState(error = e.message ?: "Error al cargar favoritos")
                }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredFavorites(): List<FavoriteDto> {
        val query = _uiState.value.searchQuery
        return if (query.isBlank()) {
            _uiState.value.favorites
        } else {
            _uiState.value.favorites.filter {
                it.channelName.contains(query, ignoreCase = true)
            }
        }
    }

    fun removeFavorite(channelId: Long) {
        viewModelScope.launch {
            tvApiClient.toggleFavorite(channelId)
                .onSuccess { loadFavorites() }
        }
    }
}
