package space.pitchstone.android.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.domain.usecase.AddCategoryUseCase
import space.pitchstone.android.domain.usecase.ObserveCategoriesUseCase
import space.pitchstone.android.domain.usecase.UpdateCategoryCapUseCase
import javax.inject.Inject

private const val CAP_STEP = 500

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val updateCategoryCapUseCase: UpdateCategoryCapUseCase,
    private val addCategoryUseCase: AddCategoryUseCase
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> = observeCategoriesUseCase()
        .map { BudgetsUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState.Loading)

    fun incrementCap(category: BudgetCategory) {
        viewModelScope.launch {
            updateCategoryCapUseCase(category.name, category.monthlyCap + CAP_STEP)
        }
    }

    fun decrementCap(category: BudgetCategory) {
        val newCap = (category.monthlyCap - CAP_STEP).coerceAtLeast(0)
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
    data class Ready(val categories: List<BudgetCategory>) : BudgetsUiState
}
