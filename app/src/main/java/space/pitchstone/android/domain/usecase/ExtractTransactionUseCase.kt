package space.pitchstone.android.domain.usecase

import javax.inject.Inject
import space.pitchstone.android.domain.model.Attachment
import space.pitchstone.android.domain.repository.TransactionRepository

class ExtractTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(text: String?, attachments: List<Attachment>): Result<String> {
        return repository.extractTransaction(text, attachments)
    }
}
