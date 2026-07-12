package space.pitchstone.android.domain.usecase

import space.pitchstone.android.domain.repository.BudgetRepository
import javax.inject.Inject

class UpdateCategoryCapUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(categoryName: String, newCap: Int) {
        repository.updateCap(categoryName, newCap)
    }
}
