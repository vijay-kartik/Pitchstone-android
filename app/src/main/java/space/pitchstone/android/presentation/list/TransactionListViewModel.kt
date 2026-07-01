package space.pitchstone.android.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.domain.usecase.GetTransactionsUseCase
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListUiState>(ListUiState.Loading)
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = ListUiState.Loading
            getTransactionsUseCase(limit = 100)
                .onSuccess { transactions ->
                    _uiState.value = ListUiState.Success(transactions)
                }
                .onFailure { error ->
                    _uiState.value = ListUiState.Error(error.message ?: "Failed to load transactions.")
                }
        }
    }
}

sealed interface ListUiState {
    object Loading : ListUiState
    data class Success(val transactions: List<Transaction>) : ListUiState
    data class Error(val message: String) : ListUiState
}
