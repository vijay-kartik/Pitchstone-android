package space.pitchstone.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.domain.repository.BudgetRepository
import javax.inject.Inject

class ObserveCategoriesUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    operator fun invoke(): Flow<List<BudgetCategory>> = repository.observeCategories()
}
