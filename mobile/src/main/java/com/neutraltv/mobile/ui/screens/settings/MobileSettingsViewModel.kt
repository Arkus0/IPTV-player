package com.neutraltv.mobile.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.neutraltv.mobile.data.remote.ConnectionState
import com.neutraltv.mobile.data.remote.TvApiClient
import com.neutraltv.mobile.data.remote.TvWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class SettingsUiState(
    val isConnected: Boolean = false,
    val connectedHost: String? = null,
    val appVersion: String = "1.0.0"
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

    fun disconnect() {
        tvWebSocketClient.disconnect()
        tvApiClient.disconnect()
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            connectedHost = null
        )
    }
}
