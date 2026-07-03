package space.pitchstone.android.data.storage

import space.pitchstone.android.domain.model.BudgetCategory

object DefaultBudgetCategories {
    val categories = listOf(
        BudgetCategory("Food", 8000, listOf("swiggy", "zomato", "pizza", "restaurant", "cafe")),
        BudgetCategory("Groceries", 5000, listOf("bigbasket", "grofers", "blinkit", "grocery", "supermarket")),
        BudgetCategory("Bills", 3000, listOf("electricity", "water", "internet", "broadband", "airtel", "jio")),
        BudgetCategory("Transport", 2000, listOf("ola", "uber", "rapido", "auto", "metro", "petrol", "fuel")),
        BudgetCategory("Rent", 25000, listOf("rent", "maintenance", "society", "housing"))
    )
}
