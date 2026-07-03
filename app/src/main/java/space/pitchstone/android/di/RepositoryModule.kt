package space.pitchstone.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import space.pitchstone.android.data.repository.BudgetRepositoryImpl
import space.pitchstone.android.data.repository.TransactionRepositoryImpl
import space.pitchstone.android.domain.repository.BudgetRepository
import space.pitchstone.android.domain.repository.TransactionRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
}
