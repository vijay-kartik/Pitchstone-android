package space.pitchstone.android.presentation.capture

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import space.pitchstone.android.domain.model.Attachment
import space.pitchstone.android.domain.usecase.ExtractTransactionUseCase
import space.pitchstone.android.presentation.util.ImageCompressor
import javax.inject.Inject

private const val IMAGE_PREP_TIMEOUT_MS = 30_000L

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

            fun addStep(step: String) {
                steps.add(step)
                _uiState.value = CaptureUiState.Processing(steps.toList())
            }

            val attachments = if (current.images.isEmpty()) {
                emptyList()
            } else {
                addStep("preparing ${current.images.size} screenshot${if (current.images.size == 1) "" else "s"}…")
                val prepared = try {
                    withTimeout(IMAGE_PREP_TIMEOUT_MS) {
                        withContext(Dispatchers.IO) {
                            current.images.mapNotNull { uri ->
                                val bytes = ImageCompressor.compressUri(context, uri) ?: return@mapNotNull null
                                val name = getFileName(uri) ?: "screenshot.jpg"
                                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                Attachment(name, bytes, mimeType)
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    _uiState.value = CaptureUiState.Error(
                        message = "Reading the selected screenshots timed out — remove and re-add them, then retry.",
                        previousInput = current
                    )
                    return@launch
                }
                if (prepared.isEmpty()) {
                    _uiState.value = CaptureUiState.Error(
                        message = "Couldn't read the selected screenshots — remove and re-add them, then retry.",
                        previousInput = current
                    )
                    return@launch
                }
                prepared
            }

            val prompt = buildPrompt(current.note, current.extractAndSave, hasImages = attachments.isNotEmpty())
            if (attachments.isEmpty()) {
                addStep("sending your question…")
            } else {
                val totalKb = attachments.sumOf { it.bytes.size } / 1024
                addStep("uploading $totalKb KB · agent is reading the image…")
            }

            extractTransactionUseCase(prompt, attachments)
                .onSuccess { replyText ->
                    addStep("reply received")
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

    // The instruction must match what is actually attached: referencing a screenshot
    // that isn't there sends the agent hunting for a missing image and stalls the run.
    private fun buildPrompt(note: String, extractAndSave: Boolean, hasImages: Boolean): String {
        val baseInstruction = when {
            extractAndSave && hasImages ->
                "Extract the transaction details from the attached screenshot(s) and return them as structured JSON — include fields like amount, currency, date/time, sender, recipient, transaction/reference ID, and status if visible. If a field isn't visible, omit it rather than guessing."

            extractAndSave ->
                "Extract the transaction details from the note above and return them as structured JSON — include fields like amount, currency, date/time, sender, recipient, transaction/reference ID, and status if mentioned. If a field isn't mentioned, omit it rather than guessing."

            hasImages ->
                "Analyse the attached screenshot(s) and answer the user's question."

            // Just-ask with no images: the note IS the question — send it verbatim.
            else -> ""
        }
        return when {
            note.isBlank() -> baseInstruction
            baseInstruction.isBlank() -> note
            else -> "$note\n\n$baseInstruction"
        }
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
