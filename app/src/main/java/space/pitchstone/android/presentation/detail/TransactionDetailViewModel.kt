package space.pitchstone.android.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.domain.usecase.GetTransactionByIdUseCase
import space.pitchstone.android.presentation.navigation.TransactionDetailRoute
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase
) : ViewModel() {

    private val route: TransactionDetailRoute = savedStateHandle.toRoute()
    val transactionId: String = route.transactionId

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadTransaction()
    }

    fun loadTransaction() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            getTransactionByIdUseCase(transactionId)
                .onSuccess { transaction ->
                    _uiState.value = DetailUiState.Success(transaction)
                }
                .onFailure { error ->
                    _uiState.value = DetailUiState.Error(error.message ?: "Failed to load transaction details.")
                }
        }
    }
}

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val transaction: Transaction) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
