package space.pitchstone.android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.pitchstone.android.domain.model.LedgerSummary
import space.pitchstone.android.domain.usecase.BuildLedgerSummaryUseCase
import space.pitchstone.android.domain.usecase.GetTransactionsUseCase
import space.pitchstone.android.domain.usecase.ObserveCategoriesUseCase
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val buildLedgerSummaryUseCase: BuildLedgerSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadLedger()
    }

    fun loadLedger() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val txnResult = getTransactionsUseCase()
                val transactions = txnResult.getOrElse { emptyList() }

                observeCategoriesUseCase().collect { categories ->
                    val summary = buildLedgerSummaryUseCase(transactions, categories)
                    _uiState.value = HomeUiState.Ready(summary)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load ledger.")
            }
        }
    }
}

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Ready(val summary: LedgerSummary) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
