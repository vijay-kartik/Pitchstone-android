package space.pitchstone.android.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object SettingsRoute

@Serializable
data class ConfirmRoute(val replyText: String)

@Serializable
object TransactionListRoute

@Serializable
data class TransactionDetailRoute(val transactionId: String)
