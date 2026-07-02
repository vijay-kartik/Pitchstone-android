package space.pitchstone.android.presentation.extraction

/**
 * Lifecycle of a background transaction extraction, shared app-wide via
 * [ExtractionCoordinator] so any screen (or the share-intent entry point)
 * can observe the same run.
 */
sealed interface ExtractionState {
    data object Idle : ExtractionState
    data object Running : ExtractionState
    data class Success(val replyText: String) : ExtractionState
    data class Error(val message: String) : ExtractionState
}
