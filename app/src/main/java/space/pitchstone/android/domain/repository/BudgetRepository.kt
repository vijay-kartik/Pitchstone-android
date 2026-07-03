package space.pitchstone.android.domain.repository

import kotlinx.coroutines.flow.Flow
import space.pitchstone.android.domain.model.BudgetCategory

interface BudgetRepository {
    fun observeCategories(): Flow<List<BudgetCategory>>
    suspend fun updateCap(categoryName: String, newCap: Int)
    suspend fun addCategory(category: BudgetCategory)
}
