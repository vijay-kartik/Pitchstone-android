package space.pitchstone.android.presentation.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import space.pitchstone.android.domain.usecase.SaveTransactionUseCase
import space.pitchstone.android.presentation.navigation.ConfirmRoute
import space.pitchstone.android.presentation.util.JsonExtractor
import javax.inject.Inject

// Helper import extension because compiler needs viewModelScope launch
import kotlinx.coroutines.launch

@HiltViewModel
class ConfirmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saveTransactionUseCase: SaveTransactionUseCase
) : ViewModel() {

    private val route: ConfirmRoute = savedStateHandle.toRoute()
    val replyText: String = route.replyText

    private val _uiState = MutableStateFlow<ConfirmUiState>(ConfirmUiState.Idle)
    val uiState: StateFlow<ConfirmUiState> = _uiState.asStateFlow()

    init {
        parseReply()
    }

    private fun parseReply() {
        _uiState.value = ConfirmUiState.Loading
        val extractor = JsonExtractor()
        val extractedMap = extractor.extractJson(replyText)
        
        if (extractedMap != null) {
            val fields = ExtractedFields(
                amount = parseStringField(extractedMap, "amount", "total", "value"),
                currency = parseStringField(extractedMap, "currency"),
                dateTime = parseStringField(extractedMap, "date_time", "transaction_date", "date"),
                sender = parseStringOrObjectField(extractedMap, "sender"),
                recipient = parseStringField(extractedMap, "recipient"),
                transactionId = parseStringField(extractedMap, "transaction_id", "reference_id"),
                status = parseStringField(extractedMap, "status"),
                rawMap = extractedMap
            )
            _uiState.value = ConfirmUiState.Extracted(fields)
        } else {
            _uiState.value = ConfirmUiState.RawText(replyText)
        }
    }

    private fun parseStringField(map: Map<String, Any?>, vararg keys: String): String {
        for (key in keys) {
            val value = map[key]
            if (value != null && value !is Map<*, *>) {
                return value.toString()
            }
        }
        return ""
    }

    private fun parseStringOrObjectField(map: Map<String, Any?>, key: String): String {
        val value = map[key] ?: return ""
        if (value is Map<*, *>) {
            return value.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        }
        return value.toString()
    }

    fun saveTransaction(fields: ExtractedFields) {
        viewModelScope.launch {
            _uiState.value = ConfirmUiState.Saving(fields)
            
            val jsonMap = fields.rawMap.toMutableMap()
            if (fields.amount.isNotBlank()) jsonMap["amount"] = fields.amount
            if (fields.currency.isNotBlank()) jsonMap["currency"] = fields.currency
            if (fields.dateTime.isNotBlank()) jsonMap["date_time"] = fields.dateTime
            if (fields.sender.isNotBlank()) jsonMap["sender"] = fields.sender
            if (fields.recipient.isNotBlank()) jsonMap["recipient"] = fields.recipient
            if (fields.transactionId.isNotBlank()) jsonMap["transaction_id"] = fields.transactionId
            if (fields.status.isNotBlank()) jsonMap["status"] = fields.status

            saveTransactionUseCase(jsonMap)
                .onSuccess {
                    _uiState.value = ConfirmUiState.Saved
                }
                .onFailure { error ->
                    _uiState.value = ConfirmUiState.SaveError(fields, error.message ?: "Failed to save transaction.")
                }
        }
    }
}

data class ExtractedFields(
    val amount: String,
    val currency: String,
    val dateTime: String,
    val sender: String,
    val recipient: String,
    val transactionId: String,
    val status: String,
    val rawMap: Map<String, Any?>
)

sealed interface ConfirmUiState {
    object Idle : ConfirmUiState
    object Loading : ConfirmUiState
    data class Extracted(val fields: ExtractedFields) : ConfirmUiState
    data class RawText(val text: String) : ConfirmUiState
    data class Saving(val fields: ExtractedFields) : ConfirmUiState
    object Saved : ConfirmUiState
    data class SaveError(val fields: ExtractedFields, val message: String) : ConfirmUiState
}
