package space.pitchstone.android.data.model

import com.google.gson.annotations.SerializedName
import space.pitchstone.android.domain.model.Transaction

data class TransactionDto(
    @SerializedName("id") val id: String,
    @SerializedName("amount") val amount: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("transaction_date") val transactionDate: String?,
    @SerializedName("sender") val sender: String?,
    @SerializedName("recipient") val recipient: String?,
    @SerializedName("reference_id") val referenceId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("raw_json") val rawJson: Map<String, Any?>?,
    @SerializedName("created_at") val createdAt: String?
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        amount = amount,
        currency = currency,
        transactionDate = transactionDate,
        sender = sender,
        recipient = recipient,
        referenceId = referenceId,
        status = status,
        rawJson = rawJson,
        createdAt = createdAt
    )
}
