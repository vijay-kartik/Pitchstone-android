package space.pitchstone.android.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import space.pitchstone.android.data.storage.DefaultBudgetCategories
import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.domain.repository.BudgetRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : BudgetRepository {

    private val prefs = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    private val categoriesKey = "categories_json"
    private val listType = object : TypeToken<List<BudgetCategory>>() {}.type

    private val _categories = MutableStateFlow(loadCategories())

    override fun observeCategories(): Flow<List<BudgetCategory>> = _categories

    override suspend fun updateCap(categoryName: String, newCap: Int) {
        val updated = _categories.value.map { cat ->
            if (cat.name == categoryName) cat.copy(monthlyCap = newCap) else cat
        }
        saveAndEmit(updated)
    }

    override suspend fun addCategory(category: BudgetCategory) {
        saveAndEmit(_categories.value + category)
    }

    private fun loadCategories(): List<BudgetCategory> {
        val json = prefs.getString(categoriesKey, null) ?: return DefaultBudgetCategories.categories
        return try {
            gson.fromJson(json, listType)
        } catch (e: Exception) {
            DefaultBudgetCategories.categories
        }
    }

    private fun saveAndEmit(categories: List<BudgetCategory>) {
        prefs.edit().putString(categoriesKey, gson.toJson(categories)).apply()
        _categories.value = categories
    }
}
