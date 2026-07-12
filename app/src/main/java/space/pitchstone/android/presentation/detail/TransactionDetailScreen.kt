package space.pitchstone.android.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.pitchstone.android.domain.model.Transaction
import space.pitchstone.android.presentation.util.formatDateTimeLong
import space.pitchstone.android.presentation.util.formatRupeesString
import space.pitchstone.android.ui.components.AccentButton
import space.pitchstone.android.ui.components.HairlineDivider
import space.pitchstone.android.ui.components.MonoPill
import space.pitchstone.android.ui.components.MonoText
import space.pitchstone.android.ui.components.ScreenHeader
import space.pitchstone.android.ui.components.SectionLabel
import space.pitchstone.android.ui.theme.JetBrainsMono
import space.pitchstone.android.ui.theme.PitchstoneColors

@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchstoneColors.Background)
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PitchstoneColors.Accent)
                }
            }

            is DetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = PitchstoneColors.Danger,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(state.message, color = PitchstoneColors.OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                    AccentButton("Retry", onClick = { viewModel.loadTransaction() })
                }
            }

            is DetailUiState.Success -> {
                DetailContent(transaction = state.transaction, onBackClick = onBackClick)
            }
        }
    }
}

@Composable
private fun DetailContent(transaction: Transaction, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        ScreenHeader(title = "Transaction", onBack = onBackClick)
        Spacer(Modifier.height(28.dp))

        val isSuccess = transaction.status?.contains("Completed", ignoreCase = true) == true ||
                transaction.status?.contains("Success", ignoreCase = true) == true

        val statusColor = if (isSuccess) PitchstoneColors.Accent else PitchstoneColors.Danger

        Text(
            text = formatRupeesString(transaction.amount),
            fontFamily = JetBrainsMono,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            color = PitchstoneColors.OnBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildString {
                if (!transaction.recipient.isNullOrBlank()) append(transaction.recipient)
                val dateStr = formatDateTimeLong(transaction.transactionDate)
                if (dateStr.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(dateStr)
                }
            },
            color = PitchstoneColors.OnSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        MonoPill(
            text = transaction.status ?: "Unknown",
            color = statusColor
        )

        Spacer(Modifier.height(28.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PitchstoneColors.Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(label = "Date & Time", value = formatDateTimeLong(transaction.transactionDate))
                HairlineDivider()
                DetailRow(label = "Sender", value = transaction.sender ?: "—")
                HairlineDivider()
                DetailRow(label = "Reference ID", value = transaction.referenceId ?: "—")
                HairlineDivider()
                DetailRow(label = "Created At", value = formatDateTimeLong(transaction.createdAt))
                HairlineDivider()
                DetailRow(label = "Database ID", value = transaction.id)
            }
        }

        if (transaction.rawJson != null) {
            Spacer(Modifier.height(24.dp))
            SectionLabel("RAW PAYLOAD")
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PitchstoneColors.SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    MonoText(
                        text = transaction.rawJson.toPrettyJson(),
                        color = PitchstoneColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = PitchstoneColors.OnSurfaceVariant,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = PitchstoneColors.OnBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun Map<String, Any?>.toPrettyJson(): String = try {
    com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(this)
} catch (e: Exception) {
    this.toString()
}
