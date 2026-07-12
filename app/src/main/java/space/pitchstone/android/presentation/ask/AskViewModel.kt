package space.pitchstone.android.presentation.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.pitchstone.android.domain.usecase.ExtractTransactionUseCase
import javax.inject.Inject

data class ChatMessage(val text: String, val isUser: Boolean)

@HiltViewModel
class AskViewModel @Inject constructor(
    private val extractTransactionUseCase: ExtractTransactionUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.value = _messages.value + ChatMessage(text, isUser = true)
        _isThinking.value = true

        viewModelScope.launch {
            extractTransactionUseCase(text, emptyList())
                .onSuccess { reply ->
                    _messages.value = _messages.value + ChatMessage(reply, isUser = false)
                }
                .onFailure { error ->
                    _messages.value = _messages.value + ChatMessage(
                        "Error: ${error.message ?: "Request failed."}",
                        isUser = false
                    )
                }
            _isThinking.value = false
        }
    }
}
