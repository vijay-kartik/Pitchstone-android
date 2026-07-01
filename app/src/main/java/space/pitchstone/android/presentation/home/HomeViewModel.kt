package space.pitchstone.android.presentation.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.pitchstone.android.domain.model.Attachment
import space.pitchstone.android.domain.usecase.ExtractTransactionUseCase
import space.pitchstone.android.presentation.util.ImageCompressor
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extractTransactionUseCase: ExtractTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    fun selectImages(uris: List<Uri>) {
        val totalList = (_selectedImageUris.value + uris).take(10) // Limit to 10 files total
        _selectedImageUris.value = totalList
    }

    fun removeImage(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value.filter { it != uri }
    }

    fun clearImages() {
        _selectedImageUris.value = emptyList()
    }

    fun clearState() {
        _uiState.value = HomeUiState.Idle
    }

    fun sendRequest(userInput: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                // Determine prompt logic
                val finalPrompt = if (_selectedImageUris.value.isNotEmpty()) {
                    val promptInstructions = "Extract the transaction details from the attached screenshot and return them as structured JSON — include fields like amount, currency, date/time, sender, recipient, transaction/reference ID, and status if visible. If a field isn't visible, omit it rather than guessing."
                    if (userInput.isBlank()) {
                        promptInstructions
                    } else {
                        "$userInput\n\n$promptInstructions"
                    }
                } else {
                    userInput
                }

                if (finalPrompt.isBlank() && _selectedImageUris.value.isEmpty()) {
                    _uiState.value = HomeUiState.Error("Please enter some text or attach an image.")
                    return@launch
                }

                // Convert Uris to Attachments in a background thread
                val attachments = withContext(Dispatchers.IO) {
                    _selectedImageUris.value.mapNotNull { uri ->
                        val bytes = ImageCompressor.compressUri(context, uri) ?: return@mapNotNull null
                        val name = getFileName(uri) ?: "screenshot.jpg"
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        Attachment(name, bytes, mimeType)
                    }
                }

                // Call extract usecase
                extractTransactionUseCase(finalPrompt, attachments)
                    .onSuccess { replyText ->
                        _uiState.value = HomeUiState.Success(replyText)
                    }
                    .onFailure { error ->
                        _uiState.value = HomeUiState.Error(error.message ?: "Failed to extract transaction details.")
                    }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "An unexpected error occurred during request prep.")
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }
}

sealed interface HomeUiState {
    object Idle : HomeUiState
    object Loading : HomeUiState
    data class Success(val replyText: String) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
