package com.neutraltv.player.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.player.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _hasPlaylist = MutableStateFlow<Boolean?>(null)
    val hasPlaylist: StateFlow<Boolean?> = _hasPlaylist

    init {
        viewModelScope.launch {
            _hasPlaylist.value = repository.hasActivePlaylist()
        }
    }
}
