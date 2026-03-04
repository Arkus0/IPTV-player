package com.neutraltv.player.ui.screens.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.EpisodeEntity
import com.neutraltv.player.data.local.entity.SeriesEntity
import com.neutraltv.player.data.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesDetailUiState(
    val series: SeriesEntity? = null,
    val seasons: List<Int> = emptyList(),
    val selectedSeason: Int = 1,
    val episodes: List<EpisodeEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState

    private val seriesId: Long = savedStateHandle.get<Long>("seriesId") ?: 0L

    init {
        loadSeriesDetail()
    }

    private fun loadSeriesDetail() {
        viewModelScope.launch {
            val series = xtreamRepository.getSeriesById(seriesId)
            if (series != null) {
                _uiState.value = _uiState.value.copy(series = series)
                val seasons = xtreamRepository.getSeasonsBySeries(seriesId)
                val firstSeason = seasons.firstOrNull() ?: 1
                _uiState.value = _uiState.value.copy(
                    seasons = seasons,
                    selectedSeason = firstSeason
                )
                loadEpisodes(firstSeason)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun selectSeason(season: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
        loadEpisodes(season)
    }

    private fun loadEpisodes(season: Int) {
        viewModelScope.launch {
            val episodes = xtreamRepository.getEpisodesBySeriesAndSeason(seriesId, season)
            _uiState.value = _uiState.value.copy(episodes = episodes)
        }
    }
}
