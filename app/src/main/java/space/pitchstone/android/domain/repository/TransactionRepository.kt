package space.pitchstone.android.domain.repository

import space.pitchstone.android.domain.model.Attachment
import space.pitchstone.android.domain.model.Transaction

interface TransactionRepository {
    suspend fun getTransactions(limit: Int): Result<List<Transaction>>
    suspend fun getTransactionById(id: String): Result<Transaction>
    suspend fun saveTransaction(rawJson: Map<String, Any?>): Result<Transaction>
    suspend fun extractTransaction(text: String?, attachments: List<Attachment>): Result<String>
    suspend fun pingGateway(): Result<Boolean>
}
