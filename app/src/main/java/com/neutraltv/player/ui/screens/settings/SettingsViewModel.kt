package com.neutraltv.player.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    suspend fun deletePlaylist() {
        repository.deleteActivePlaylist()
    }
}
