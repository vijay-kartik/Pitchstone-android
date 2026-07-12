package space.pitchstone.android.domain.service

import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.domain.model.CategorySpend
import space.pitchstone.android.domain.model.LedgerSummary
import space.pitchstone.android.domain.model.Transaction
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class LedgerCalculator @Inject constructor(
    private val clock: Clock,
    private val categorizer: TransactionCategorizer
) {
    fun calculate(transactions: List<Transaction>, categories: List<BudgetCategory>): LedgerSummary {
        val now = LocalDate.now(clock)
        val monthLabel = "${now.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${now.year}"

        val currentMonthTxns = transactions.filter { txn ->
            val dateStr = txn.transactionDate ?: txn.createdAt ?: return@filter false
            try {
                val date = LocalDate.parse(dateStr.substringBefore("T"))
                date.year == now.year && date.monthValue == now.monthValue
            } catch (e: Exception) {
                false
            }
        }

        val categorySpends = categories.map { category ->
            val spent = currentMonthTxns
                .filter { txn -> categorizer.categorize(txn, listOf(category)) != null }
                .sumOf { txn -> AmountParser.parse(txn.amount) }
            val cap = category.monthlyCap.coerceAtLeast(1)
            CategorySpend(
                category = category,
                spent = spent,
                ratio = (spent.toFloat() / cap).coerceIn(0f, 1.5f),
                isOver = spent > cap,
                remaining = (cap - spent).coerceAtLeast(0)
            )
        }

        val monthSpent = currentMonthTxns.sumOf { AmountParser.parse(it.amount) }

        // Fraction of the calendar month elapsed — drives the pace tick so a
        // spend bar sitting past the tick reads as "burning faster than time".
        val monthProgressFraction = now.dayOfMonth.toFloat() / now.lengthOfMonth()

        return LedgerSummary(
            monthLabel = monthLabel,
            monthSpent = monthSpent,
            monthProgressFraction = monthProgressFraction.coerceIn(0f, 1f),
            categories = categorySpends,
            recentTransactions = transactions.take(5)
        )
    }
}
