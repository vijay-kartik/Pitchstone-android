package space.pitchstone.android.presentation.util

import space.pitchstone.android.domain.model.Transaction
import java.text.NumberFormat
import java.util.Locale

private val indianLocale = Locale("en", "IN")
private val rupeeFormat: NumberFormat = NumberFormat.getNumberInstance(indianLocale)

fun formatRupees(amount: Int): String = "₹${rupeeFormat.format(amount)}"

fun formatRupeesString(amount: String?): String {
    if (amount.isNullOrBlank()) return "₹0"
    val numeric = amount.replace(",", "").replace("₹", "").trim()
    val long = numeric.split(".")[0].toLongOrNull() ?: return "₹$amount"
    return "₹${rupeeFormat.format(long)}"
}

fun formatDateTimeLong(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "—"
    return try {
        if (isoString.contains("T")) {
            val date = isoString.substringBefore("T")
            val time = isoString.substringAfter("T").take(5)
            "$date · $time"
        } else isoString
    } catch (e: Exception) {
        isoString
    }
}

fun formatShortDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "—"
    return isoString.substringBefore("T").ifBlank { isoString }
}

fun transactionMeta(transaction: Transaction): String {
    val parts = listOfNotNull(
        transaction.recipient?.takeIf { it.isNotBlank() },
        formatShortDate(transaction.transactionDate ?: transaction.createdAt)
    )
    return parts.joinToString(" · ")
}
