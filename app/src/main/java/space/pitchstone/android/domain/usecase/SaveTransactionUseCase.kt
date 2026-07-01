package space.pitchstone.android.domain.usecase

import javax.inject.Inject
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.domain.repository.TransactionRepository

class SaveTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(rawJson: Map<String, Any?>): Result<Transaction> {
        return repository.saveTransaction(rawJson)
    }
}
