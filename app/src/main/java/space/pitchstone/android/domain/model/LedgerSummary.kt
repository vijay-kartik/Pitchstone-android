package space.pitchstone.android.domain.model

data class LedgerSummary(
    val monthLabel: String,
    val monthSpent: Int,
    val monthProgressFraction: Float,
    val categories: List<CategorySpend>,
    val recentTransactions: List<Transaction>
)
