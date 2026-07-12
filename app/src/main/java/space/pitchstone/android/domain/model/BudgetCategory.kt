package space.pitchstone.android.domain.model

data class BudgetCategory(
    val name: String,
    val monthlyCap: Int,
    val keywords: List<String>
)
