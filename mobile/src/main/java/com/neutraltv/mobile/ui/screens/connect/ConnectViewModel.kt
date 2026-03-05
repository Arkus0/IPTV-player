package com.neutraltv.mobile.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neutraltv.core.discovery.DiscoveredDevice
import com.neutraltv.core.discovery.NsdDiscoveryManager
import com.neutraltv.mobile.data.remote.ConnectionState
import com.neutraltv.mobile.data.remote.TvApiClient
import com.neutraltv.mobile.data.remote.TvWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val devices: List<DiscoveredDevice> = emptyList(),
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val error: String? = null
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val nsdManager: NsdDiscoveryManager,
    private val tvApiClient: TvApiClient,
    private val tvWebSocketClient: TvWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                nsdManager.discoveredDevices,
                nsdManager.isDiscovering,
                tvWebSocketClient.connectionState
            ) { devices, isDiscovering, wsState ->
                _uiState.value.copy(
                    devices = devices,
                    isScanning = isDiscovering,
                    isConnected = wsState == ConnectionState.CONNECTED
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startScan() {
        nsdManager.startDiscovery()
    }

    fun stopScan() {
        nsdManager.stopDiscovery()
    }

    fun connectToDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)

            tvApiClient.setServer(device.host, device.port)
            val result = tvApiClient.getDeviceInfo()

            result.fold(
                onSuccess = { info ->
                    tvWebSocketClient.connect(device.host, device.port)
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isConnected = true,
                        connectedDeviceName = info.deviceName
                    )
                    stopScan()
                },
                onFailure = { e ->
                    tvApiClient.disconnect()
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = "No se pudo conectar: ${e.message}"
                    )
                }
            )
        }
    }

    fun connectManual(host: String, port: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)

            tvApiClient.setServer(host, port)
            val result = tvApiClient.getDeviceInfo()

            result.fold(
                onSuccess = { info ->
                    tvWebSocketClient.connect(host, port)
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isConnected = true,
                        connectedDeviceName = info.deviceName
                    )
                    stopScan()
                },
                onFailure = { e ->
                    tvApiClient.disconnect()
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = "No se pudo conectar: ${e.message}"
                    )
                }
            )
        }
    }

    fun disconnect() {
        tvWebSocketClient.disconnect()
        tvApiClient.disconnect()
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            connectedDeviceName = null
        )
    }

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }
}
