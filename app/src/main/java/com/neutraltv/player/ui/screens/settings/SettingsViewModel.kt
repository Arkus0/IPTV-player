package com.neutraltv.player.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.backup.BackupManager
import com.neutraltv.player.data.preferences.PreferencesRepository
import com.neutraltv.player.data.preferences.UserPreferences
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentTheme: String = "purple_dark",
    val currentFontScale: Float = 1.0f,
    val customEpgUrl: String = "",
    val companionModeEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val preferencesRepository: PreferencesRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            preferencesRepository.getUserPreferences().collect { prefs ->
                _uiState.value = SettingsUiState(
                    currentTheme = prefs.themeId,
                    currentFontScale = prefs.fontScale,
                    customEpgUrl = prefs.customEpgUrl,
                    companionModeEnabled = prefs.companionModeEnabled
                )
            }
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            preferencesRepository.setTheme(themeId)
        }
    }

    fun selectFontScale(scale: Float) {
        viewModelScope.launch {
            preferencesRepository.setFontScale(scale)
        }
    }

    fun saveCustomEpgUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.setCustomEpgUrl(url.trim())
        }
    }

    fun toggleCompanionMode() {
        viewModelScope.launch {
            preferencesRepository.setCompanionMode(!_uiState.value.companionModeEnabled)
        }
    }

    suspend fun deletePlaylist() {
        repository.deleteActivePlaylist()
    }

    suspend fun exportBackup(): String {
        return backupManager.exportBackup()
    }

    suspend fun importBackup(json: String): Result<Unit> {
        return backupManager.importBackup(json).map { }
    }
}
