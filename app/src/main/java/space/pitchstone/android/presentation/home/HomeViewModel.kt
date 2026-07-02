package space.pitchstone.android.presentation.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import space.pitchstone.android.presentation.extraction.ExtractionCoordinator
import space.pitchstone.android.presentation.extraction.ExtractionState
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val extractionCoordinator: ExtractionCoordinator
) : ViewModel() {

    private val _validationError = MutableStateFlow<String?>(null)

    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        extractionCoordinator.state,
        _validationError
    ) { extractionState, validationError ->
        validationError?.let { HomeUiState.Error(it) } ?: extractionState.toHomeUiState()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Idle)

    fun selectImages(uris: List<Uri>) {
        _selectedImageUris.value = (_selectedImageUris.value + uris).take(MAX_ATTACHMENTS)
    }

    fun removeImage(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value.filter { it != uri }
    }

    fun sendRequest(userInput: String) {
        _validationError.value = null
        if (userInput.isBlank() && _selectedImageUris.value.isEmpty()) {
            _validationError.value = "Please enter some text or attach an image."
            return
        }
        extractionCoordinator.start(userInput, _selectedImageUris.value)
    }

    /** Called once the UI has navigated with the extraction result. */
    fun onExtractionResultConsumed() {
        extractionCoordinator.consumeResult()
        _selectedImageUris.value = emptyList()
    }

    private fun ExtractionState.toHomeUiState(): HomeUiState = when (this) {
        ExtractionState.Idle -> HomeUiState.Idle
        ExtractionState.Running -> HomeUiState.Loading
        is ExtractionState.Success -> HomeUiState.Success(replyText)
        is ExtractionState.Error -> HomeUiState.Error(message)
    }

    private companion object {
        const val MAX_ATTACHMENTS = 10
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState
    data class Success(val replyText: String) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
