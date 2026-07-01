package space.pitchstone.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.pitchstone.android.data.storage.GatewayPreferences
import space.pitchstone.android.domain.usecase.PingGatewayUseCase
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: GatewayPreferences,
    private val pingGatewayUseCase: PingGatewayUseCase
) : ViewModel() {

    val baseUrl: StateFlow<String> = preferences.getBaseUrl()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val apiKey: StateFlow<String> = preferences.getApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    fun updateBaseUrl(url: String) {
        viewModelScope.launch {
            preferences.setBaseUrl(url)
            _connectionStatus.value = ConnectionStatus.Idle
        }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            preferences.setApiKey(key)
            _connectionStatus.value = ConnectionStatus.Idle
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Checking
            pingGatewayUseCase()
                .onSuccess { isReachable ->
                    if (isReachable) {
                        _connectionStatus.value = ConnectionStatus.Success
                    } else {
                        _connectionStatus.value = ConnectionStatus.Failure("Gateway returned unhealthy status.")
                    }
                }
                .onFailure { error ->
                    _connectionStatus.value = ConnectionStatus.Failure(error.message ?: "Failed to connect to gateway.")
                }
        }
    }

    sealed interface ConnectionStatus {
        object Idle : ConnectionStatus
        object Checking : ConnectionStatus
        object Success : ConnectionStatus
        data class Failure(val error: String) : ConnectionStatus
    }
}
