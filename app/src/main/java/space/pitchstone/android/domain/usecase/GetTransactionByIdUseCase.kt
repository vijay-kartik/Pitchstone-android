package space.pitchstone.android.domain.usecase

import javax.inject.Inject
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.domain.repository.TransactionRepository

class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: String): Result<Transaction> {
        return repository.getTransactionById(id)
    }
}
