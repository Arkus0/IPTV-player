package com.neutraltv.player.ui.screens.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.local.entity.ChannelEntity
import com.neutraltv.player.data.local.entity.ProgramEntity
import com.neutraltv.player.data.preferences.PreferencesRepository
import com.neutraltv.player.data.repository.EpgRepository
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpgUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val programs: Map<String, List<ProgramEntity>> = emptyMap(),
    val isLoading: Boolean = true,
    val isLoadingEpg: Boolean = false,
    val epgLoadError: String? = null,
    val focusedProgram: ProgramEntity? = null,
    val windowStartTime: Long = 0,
    val windowEndTime: Long = 0
)

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val epgRepository: EpgRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpgUiState())
    val uiState: StateFlow<EpgUiState> = _uiState

    private var programsJob: Job? = null
    private var currentPlaylistId: Long? = null
    private var currentEpgUrl: String? = null

    init {
        viewModelScope.launch {
            playlistRepository.getActivePlaylist().collect { playlist ->
                if (playlist != null) {
                    val epgUrl = playlist.epgUrl
                        ?: preferencesRepository.getUserPreferences().first().customEpgUrl.takeIf { it.isNotBlank() }
                    loadChannelsAndPrograms(playlist.id, epgUrl)
                } else {
                    _uiState.value = EpgUiState(isLoading = false)
                }
            }
        }
    }

    private fun loadChannelsAndPrograms(playlistId: Long, epgUrl: String?) {
        currentPlaylistId = playlistId
        currentEpgUrl = epgUrl

        val now = System.currentTimeMillis()
        val hourMs = 3600_000L
        val windowStart = now - hourMs     // 1 hour before
        val windowEnd = now + 2 * hourMs   // 2 hours after

        _uiState.value = _uiState.value.copy(
            windowStartTime = windowStart,
            windowEndTime = windowEnd
        )

        viewModelScope.launch {
            val channels = playlistRepository.getVisibleChannelsOnce(playlistId)
                .filter { it.epgChannelId != null }

            _uiState.value = _uiState.value.copy(
                channels = channels,
                isLoading = false
            )

            // Start observing programs reactively FIRST — Room Flow will re-emit on inserts
            val epgChannelIds = channels.mapNotNull { it.epgChannelId }
            if (epgChannelIds.isNotEmpty()) {
                programsJob?.cancel()
                programsJob = viewModelScope.launch {
                    epgRepository.getProgramsForChannelsInRange(epgChannelIds, windowStart, windowEnd)
                        .collect { allPrograms ->
                            val grouped = allPrograms.groupBy { it.epgChannelId }
                            _uiState.value = _uiState.value.copy(programs = grouped)
                        }
                }
            }

            // THEN trigger EPG network fetch — when data is inserted, the Flow above will re-emit
            if (epgUrl != null) {
                loadEpgData(playlistId, epgUrl)
            }
        }
    }

    private fun loadEpgData(playlistId: Long, epgUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingEpg = true)
            val result = epgRepository.loadEpg(playlistId, epgUrl)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoadingEpg = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingEpg = false,
                        epgLoadError = e.message
                    )
                }
            )
        }
    }

    fun onProgramFocused(program: ProgramEntity?) {
        _uiState.value = _uiState.value.copy(focusedProgram = program)
    }

    fun forceRefreshEpg() {
        val playlistId = currentPlaylistId ?: return
        val epgUrl = currentEpgUrl ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingEpg = true)
            val result = epgRepository.forceRefreshEpg(playlistId, epgUrl)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoadingEpg = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingEpg = false,
                        epgLoadError = e.message
                    )
                }
            )
        }
    }
}
