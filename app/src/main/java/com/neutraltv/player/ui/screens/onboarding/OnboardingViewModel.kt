package com.neutraltv.player.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val name: String = "Mi Lista",
    val url: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(url = url, error = null)
    }

    fun loadPlaylist() {
        val url = _uiState.value.url.trim()
        val name = _uiState.value.name.trim().ifBlank { "Mi Lista" }

        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Ingresa una URL válida")
            return
        }
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            _uiState.value = _uiState.value.copy(error = "La URL debe empezar con http:// o https://")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = repository.loadPlaylistFromUrl(name = name, url = url)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            )
        }
    }

    fun loadPlaylistFromContent(content: String, filePath: String?) {
        val name = _uiState.value.name.trim().ifBlank { "Mi Lista" }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = repository.loadPlaylistFromContent(
                name = name,
                content = content,
                filePath = filePath
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            )
        }
    }
}
