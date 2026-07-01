package space.pitchstone.android.domain.model

data class Transaction(
    val id: String,
    val amount: String?,
    val currency: String?,
    val transactionDate: String?,
    val sender: String?,
    val recipient: String?,
    val referenceId: String?,
    val status: String?,
    val rawJson: Map<String, Any?>?,
    val createdAt: String?
)
