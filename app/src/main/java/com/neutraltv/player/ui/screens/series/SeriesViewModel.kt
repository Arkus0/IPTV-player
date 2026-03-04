package com.neutraltv.player.ui.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.SeriesEntity
import com.neutraltv.player.data.repository.PlaylistRepository
import com.neutraltv.player.data.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesUiState(
    val series: List<SeriesEntity> = emptyList(),
    val categories: List<String?> = emptyList(),
    val selectedCategory: String? = null,
    val isAllSelected: Boolean = true,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true,
    val playlistId: Long = 0
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            playlistRepository.getActivePlaylist().collect { playlist ->
                if (playlist != null) {
                    _uiState.value = _uiState.value.copy(playlistId = playlist.id)
                    loadCategories(playlist.id)
                    loadAllSeries(playlist.id)
                }
            }
        }
    }

    private fun loadAllSeries(playlistId: Long) {
        viewModelScope.launch {
            xtreamRepository.getSeriesByPlaylist(playlistId).collect { series ->
                _uiState.value = _uiState.value.copy(
                    series = series,
                    isLoading = false
                )
            }
        }
    }

    private fun loadCategories(playlistId: Long) {
        viewModelScope.launch {
            val categories = xtreamRepository.getSeriesCategories(playlistId)
            _uiState.value = _uiState.value.copy(categories = categories)
        }
    }

    fun selectAll() {
        _uiState.value = _uiState.value.copy(
            isAllSelected = true,
            selectedCategory = null,
            isSearchActive = false,
            searchQuery = ""
        )
        viewModelScope.launch {
            xtreamRepository.getSeriesByPlaylist(_uiState.value.playlistId).collect { series ->
                _uiState.value = _uiState.value.copy(series = series)
            }
        }
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(
            isAllSelected = false,
            selectedCategory = category,
            isSearchActive = false,
            searchQuery = ""
        )
        viewModelScope.launch {
            val series = xtreamRepository.getSeriesByCategory(_uiState.value.playlistId, category)
            _uiState.value = _uiState.value.copy(series = series)
        }
    }

    fun toggleSearch() {
        val newActive = !_uiState.value.isSearchActive
        _uiState.value = _uiState.value.copy(
            isSearchActive = newActive,
            searchQuery = if (!newActive) "" else _uiState.value.searchQuery
        )
        if (!newActive) {
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
                val results = xtreamRepository.searchSeries(_uiState.value.playlistId, query)
                _uiState.value = _uiState.value.copy(series = results)
            }
        }
    }
}
