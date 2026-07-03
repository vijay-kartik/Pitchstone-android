package space.pitchstone.android.domain.model

data class CategorySpend(
    val category: BudgetCategory,
    val spent: Int,
    val ratio: Float,
    val isOver: Boolean,
    val remaining: Int
)
