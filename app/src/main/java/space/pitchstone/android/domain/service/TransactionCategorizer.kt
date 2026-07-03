package space.pitchstone.android.domain.service

import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.domain.model.Transaction
import javax.inject.Inject

class TransactionCategorizer @Inject constructor() {
    fun categorize(transaction: Transaction, categories: List<BudgetCategory>): BudgetCategory? {
        val searchText = "${transaction.recipient.orEmpty()} ${transaction.sender.orEmpty()}".lowercase()
        return categories.firstOrNull { category ->
            category.keywords.any { keyword -> searchText.contains(keyword.lowercase()) }
        }
    }
}
