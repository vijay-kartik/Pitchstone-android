package space.pitchstone.android.domain.usecase

import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.domain.repository.BudgetRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(category: BudgetCategory) {
        repository.addCategory(category)
    }
}
