package com.neutraltv.mobile.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.mobile.data.remote.TvApiClient
import com.neutraltv.mobile.data.remote.TvWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isConnected: Boolean = false,
    val connectedHost: String? = null,
    val appVersion: String = "1.0.0",
    val playlistUrl: String = "",
    val playlistName: String = "",
    val isAddingPlaylist: Boolean = false,
    val addPlaylistSuccess: String? = null,
    val addPlaylistError: String? = null
)

@HiltViewModel
class MobileSettingsViewModel @Inject constructor(
    private val tvApiClient: TvApiClient,
    private val tvWebSocketClient: TvWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun refresh() {
        _uiState.value = _uiState.value.copy(
            isConnected = tvApiClient.isConnected
        )
    }

    fun updatePlaylistUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            playlistUrl = url,
            addPlaylistError = null,
            addPlaylistSuccess = null
        )
    }

    fun updatePlaylistName(name: String) {
        _uiState.value = _uiState.value.copy(playlistName = name)
    }

    fun addPlaylistToTv() {
        val url = _uiState.value.playlistUrl.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(addPlaylistError = "Pega una URL de playlist")
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _uiState.value = _uiState.value.copy(addPlaylistError = "La URL debe empezar con http:// o https://")
            return
        }

        val name = _uiState.value.playlistName.trim().ifBlank { "Mi Lista" }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingPlaylist = true, addPlaylistError = null, addPlaylistSuccess = null)
            tvApiClient.addPlaylist(url, name)
                .onSuccess { playlist ->
                    _uiState.value = _uiState.value.copy(
                        isAddingPlaylist = false,
                        addPlaylistSuccess = "Playlist '${playlist.name}' agregada con ${playlist.channelCount} canales",
                        playlistUrl = "",
                        playlistName = ""
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isAddingPlaylist = false,
                        addPlaylistError = e.message ?: "Error al agregar playlist"
                    )
                }
        }
    }

    fun disconnect() {
        tvWebSocketClient.disconnect()
        tvApiClient.disconnect()
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            connectedHost = null
        )
    }
}
