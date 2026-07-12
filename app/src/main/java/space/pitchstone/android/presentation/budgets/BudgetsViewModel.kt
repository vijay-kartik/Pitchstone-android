package space.pitchstone.android.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.domain.model.CategorySpend
import space.pitchstone.android.domain.usecase.AddCategoryUseCase
import space.pitchstone.android.domain.usecase.BuildLedgerSummaryUseCase
import space.pitchstone.android.domain.usecase.GetTransactionsUseCase
import space.pitchstone.android.domain.usecase.ObserveCategoriesUseCase
import space.pitchstone.android.domain.usecase.UpdateCategoryCapUseCase
import javax.inject.Inject

private const val CAP_STEP = 500

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val buildLedgerSummaryUseCase: BuildLedgerSummaryUseCase,
    private val updateCategoryCapUseCase: UpdateCategoryCapUseCase,
    private val addCategoryUseCase: AddCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<BudgetsUiState>(BudgetsUiState.Loading)
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val transactions = getTransactionsUseCase().getOrElse { emptyList() }
            observeCategoriesUseCase().collect { categories ->
                val summary = buildLedgerSummaryUseCase(transactions, categories)
                _uiState.value = BudgetsUiState.Ready(
                    categories = summary.categories,
                    monthProgressFraction = summary.monthProgressFraction
                )
            }
        }
    }

    fun incrementCap(category: BudgetCategory) {
        viewModelScope.launch {
            updateCategoryCapUseCase(category.name, category.monthlyCap + CAP_STEP)
        }
    }

    fun decrementCap(category: BudgetCategory) {
        val newCap = (category.monthlyCap - CAP_STEP).coerceAtLeast(CAP_STEP)
        viewModelScope.launch {
            updateCategoryCapUseCase(category.name, newCap)
        }
    }

    fun addCategory(name: String, monthlyCap: Int, keywords: List<String>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            addCategoryUseCase(BudgetCategory(name.trim(), monthlyCap, keywords))
        }
    }
}

sealed interface BudgetsUiState {
    object Loading : BudgetsUiState
    data class Ready(
        val categories: List<CategorySpend>,
        val monthProgressFraction: Float
    ) : BudgetsUiState
}
