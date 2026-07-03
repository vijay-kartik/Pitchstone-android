package space.pitchstone.android.presentation.capture

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
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extractTransactionUseCase: ExtractTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Input())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun selectImages(uris: List<Uri>) {
        val current = _uiState.value as? CaptureUiState.Input ?: return
        val updated = (current.images + uris).take(10)
        _uiState.value = current.copy(images = updated)
    }

    fun removeImage(uri: Uri) {
        val current = _uiState.value as? CaptureUiState.Input ?: return
        _uiState.value = current.copy(images = current.images.filter { it != uri })
    }

    fun updateNote(note: String) {
        val current = _uiState.value as? CaptureUiState.Input ?: return
        _uiState.value = current.copy(note = note)
    }

    fun setMode(extractAndSave: Boolean) {
        val current = _uiState.value as? CaptureUiState.Input ?: return
        _uiState.value = current.copy(extractAndSave = extractAndSave)
    }

    fun runAgent() {
        val current = _uiState.value as? CaptureUiState.Input ?: return

        viewModelScope.launch {
            val steps = mutableListOf<String>()

            fun addStep(step: String): List<String> {
                steps.add(step)
                _uiState.value = CaptureUiState.Processing(steps.toList())
                return steps.toList()
            }

            addStep("Preparing images…")
            val attachments = withContext(Dispatchers.IO) {
                current.images.mapNotNull { uri ->
                    val bytes = ImageCompressor.compressUri(context, uri) ?: return@mapNotNull null
                    val name = getFileName(uri) ?: "screenshot.jpg"
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    Attachment(name, bytes, mimeType)
                }
            }

            val prompt = buildPrompt(current.note, current.extractAndSave)
            addStep("Sending to agent…")

            extractTransactionUseCase(prompt, attachments)
                .onSuccess { replyText ->
                    addStep("Agent replied.")
                    _uiState.value = CaptureUiState.Done(
                        replyText = replyText,
                        extractAndSave = current.extractAndSave
                    )
                }
                .onFailure { error ->
                    _uiState.value = CaptureUiState.Error(
                        message = error.message ?: "Agent request failed.",
                        previousInput = current
                    )
                }
        }
    }

    fun retry() {
        val errorState = _uiState.value as? CaptureUiState.Error ?: return
        _uiState.value = errorState.previousInput
    }

    private fun buildPrompt(note: String, extractAndSave: Boolean): String {
        val baseInstruction = if (extractAndSave) {
            "Extract the transaction details from the attached screenshot and return them as structured JSON — include fields like amount, currency, date/time, sender, recipient, transaction/reference ID, and status if visible. If a field isn't visible, omit it rather than guessing."
        } else {
            "Analyse the attached screenshot and answer the user's question."
        }
        return if (note.isBlank()) baseInstruction else "$note\n\n$baseInstruction"
    }

    private fun getFileName(uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) cursor.getString(index) else null
            } else null
        }
    } catch (e: Exception) {
        null
    }
}

sealed interface CaptureUiState {
    data class Input(
        val images: List<Uri> = emptyList(),
        val note: String = "",
        val extractAndSave: Boolean = true
    ) : CaptureUiState

    data class Processing(val steps: List<String>) : CaptureUiState

    data class Done(val replyText: String, val extractAndSave: Boolean) : CaptureUiState

    data class Error(val message: String, val previousInput: Input) : CaptureUiState
}
