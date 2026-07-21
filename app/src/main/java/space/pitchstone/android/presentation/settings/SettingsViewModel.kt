package space.pitchstone.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.pitchstone.android.data.storage.GatewayPreferences
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: GatewayPreferences
) : ViewModel() {

    val openAiApiKey: StateFlow<String> = preferences.getOpenAiApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val openAiModel: StateFlow<String> = preferences.getOpenAiModel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun updateOpenAiApiKey(key: String) {
        viewModelScope.launch { preferences.setOpenAiApiKey(key) }
    }

    fun updateOpenAiModel(model: String) {
        viewModelScope.launch { preferences.setOpenAiModel(model) }
    }
}
