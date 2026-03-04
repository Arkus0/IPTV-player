package com.neutraltv.player.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.repository.PlaylistRepository
import com.neutraltv.player.data.repository.XtreamRepository
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
    val isSuccess: Boolean = false,
    val isXtreamMode: Boolean = false,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = ""
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(url = url, error = null)
    }

    fun setXtreamMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isXtreamMode = enabled, error = null)
    }

    fun onServerUrlChanged(serverUrl: String) {
        _uiState.value = _uiState.value.copy(serverUrl = serverUrl, error = null)
    }

    fun onUsernameChanged(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun connectXtream() {
        val serverUrl = _uiState.value.serverUrl.trim().trimEnd('/')
        val username = _uiState.value.username.trim()
        val password = _uiState.value.password.trim()
        val name = _uiState.value.name.trim().ifBlank { "Mi Lista" }

        if (serverUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Ingresa la URL del servidor")
            return
        }
        if (!serverUrl.startsWith("http://", ignoreCase = true) &&
            !serverUrl.startsWith("https://", ignoreCase = true)
        ) {
            _uiState.value = _uiState.value.copy(error = "La URL debe empezar con http:// o https://")
            return
        }
        if (username.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Ingresa el usuario")
            return
        }
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Ingresa la contraseña")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = xtreamRepository.loadFullPlaylist(
                serverUrl = serverUrl,
                username = username,
                password = password,
                playlistName = name
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
