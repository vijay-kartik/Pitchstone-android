package space.pitchstone.android.domain.usecase

import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.domain.model.LedgerSummary
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.domain.service.LedgerCalculator
import javax.inject.Inject

class BuildLedgerSummaryUseCase @Inject constructor(
    private val calculator: LedgerCalculator
) {
    operator fun invoke(transactions: List<Transaction>, categories: List<BudgetCategory>): LedgerSummary =
        calculator.calculate(transactions, categories)
}
