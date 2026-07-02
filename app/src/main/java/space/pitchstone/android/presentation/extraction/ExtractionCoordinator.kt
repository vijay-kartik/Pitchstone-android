package space.pitchstone.android.presentation.extraction

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.pitchstone.android.di.ApplicationScope
import space.pitchstone.android.domain.usecase.ExtractTransactionUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs transaction extractions in an application-scoped coroutine so they keep
 * going when the user leaves the screen (or the app), and mirrors progress into
 * a dismissable notification via [ExtractionNotifier]. Screens observe [state]
 * to reflect the run without blocking the UI.
 */
@Singleton
class ExtractionCoordinator @Inject constructor(
    private val extractTransactionUseCase: ExtractTransactionUseCase,
    private val attachmentPreparer: AttachmentPreparer,
    private val notifier: ExtractionNotifier,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    private val _state = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val state: StateFlow<ExtractionState> = _state.asStateFlow()

    /**
     * Starts a background extraction for [userInput] plus the files at
     * [attachmentUris]. Only one extraction runs at a time; returns false if
     * one is already in flight.
     */
    fun start(userInput: String, attachmentUris: List<Uri>): Boolean {
        if (_state.value is ExtractionState.Running) return false
        _state.value = ExtractionState.Running
        applicationScope.launch {
            notifier.showInProgress()
            runExtraction(userInput, attachmentUris)
        }
        return true
    }

    /**
     * Resets a finished run back to [ExtractionState.Idle] once its result has
     * been handed to the UI, and clears the result notification.
     */
    fun consumeResult() {
        if (_state.value is ExtractionState.Running) return
        _state.value = ExtractionState.Idle
        notifier.cancel()
    }

    private suspend fun runExtraction(userInput: String, attachmentUris: List<Uri>) {
        try {
            val attachments = attachmentPreparer.prepare(attachmentUris)
            if (attachmentUris.isNotEmpty() && attachments.isEmpty()) {
                reportFailure("The attached files could not be read or are not supported.")
                return
            }
            val prompt = buildPrompt(userInput, attachments.isNotEmpty())
            if (prompt.isBlank() && attachments.isEmpty()) {
                reportFailure("Please enter some text or attach a file.")
                return
            }
            extractTransactionUseCase(prompt, attachments)
                .onSuccess { replyText ->
                    _state.value = ExtractionState.Success(replyText)
                    notifier.showSuccess(replyText)
                }
                .onFailure { error ->
                    reportFailure(error.message ?: "Failed to extract transaction details.")
                }
        } catch (e: Exception) {
            reportFailure(e.message ?: "An unexpected error occurred during request prep.")
        }
    }

    private fun reportFailure(message: String) {
        _state.value = ExtractionState.Error(message)
        notifier.showFailure(message)
    }

    private fun buildPrompt(userInput: String, hasAttachments: Boolean): String {
        if (!hasAttachments) return userInput
        return if (userInput.isBlank()) {
            EXTRACTION_INSTRUCTIONS
        } else {
            "$userInput\n\n$EXTRACTION_INSTRUCTIONS"
        }
    }

    private companion object {
        const val EXTRACTION_INSTRUCTIONS =
            "Extract the transaction details from the attached file(s) and return them as " +
                "structured JSON — include fields like amount, currency, date/time, sender, " +
                "recipient, transaction/reference ID, and status if visible. If a field isn't " +
                "visible, omit it rather than guessing."
    }
}
