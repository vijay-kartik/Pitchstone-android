package space.pitchstone.android.domain.usecase

import javax.inject.Inject
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.domain.repository.TransactionRepository

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(limit: Int = 50): Result<List<Transaction>> {
        return repository.getTransactions(limit)
    }
}
