package com.neutraltv.player.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.PlaylistEntity
import com.neutraltv.player.data.local.model.RecommendationSection
import com.neutraltv.player.data.repository.FavoriteRepository
import com.neutraltv.player.data.repository.PlaylistRepository
import com.neutraltv.player.data.repository.RecommendationEngine
import com.neutraltv.player.data.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val isLoading: Boolean = true,
    val recommendationSections: List<RecommendationSection> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val currentProgramsMap: Map<String, String> = emptyMap(),
    val isRefreshing: Boolean = false,
    val refreshMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val xtreamRepository: XtreamRepository,
    private val recommendationEngine: RecommendationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var recentJob: Job? = null
    private var favoritesJob: Job? = null
    private var recommendationsJob: Job? = null

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
                    loadRecommendations(playlist.id)
                }
            }
        }
    }

    private fun loadRecentChannels(playlistId: Long) {
        recentJob?.cancel()
        recentJob = viewModelScope.launch {
            repository.getRecentlyWatched(playlistId, 5).collect { channels ->
                _uiState.value = _uiState.value.copy(recentChannels = channels)
            }
        }
    }

    private fun loadFavoriteChannels(playlistId: Long) {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
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

    private fun loadRecommendations(playlistId: Long) {
        recommendationsJob?.cancel()
        recommendationsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRecommendations = true)
            try {
                val recent = _uiState.value.recentChannels
                val favorites = _uiState.value.favoriteChannels

                val sections = recommendationEngine.generateRecommendations(
                    playlistId = playlistId,
                    recentChannels = recent,
                    favoriteChannels = favorites
                )

                val allEpgIds = (recent + favorites)
                    .mapNotNull { it.epgChannelId }
                    .distinct()
                val programsMap = if (allEpgIds.isNotEmpty()) {
                    recommendationEngine.getCurrentProgramTitles(
                        allEpgIds, System.currentTimeMillis()
                    )
                } else emptyMap()

                _uiState.value = _uiState.value.copy(
                    recommendationSections = sections,
                    isLoadingRecommendations = false,
                    currentProgramsMap = programsMap
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingRecommendations = false)
            }
        }
    }

    fun refreshRecommendations() {
        val playlist = _uiState.value.playlist ?: return
        loadRecommendations(playlist.id)
    }

    fun refreshPlaylist() {
        val playlist = _uiState.value.playlist ?: return
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                refreshMessage = null
            )

            val result = when (playlist.type) {
                "xtream" -> xtreamRepository.refreshActivePlaylist()
                else -> repository.refreshActivePlaylist()
            }

            result.fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        refreshMessage = "success:$count"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        refreshMessage = "error:${error.message}"
                    )
                }
            )
        }
    }

    fun clearRefreshMessage() {
        _uiState.value = _uiState.value.copy(refreshMessage = null)
    }
}
