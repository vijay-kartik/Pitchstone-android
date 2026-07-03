package space.pitchstone.android.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.pitchstone.android.domain.model.CategorySpend
import space.pitchstone.android.domain.model.LedgerSummary
import space.pitchstone.android.presentation.util.formatRupees
import space.pitchstone.android.presentation.util.formatRupeesString
import space.pitchstone.android.presentation.util.transactionMeta
import space.pitchstone.android.ui.components.AccentButton
import space.pitchstone.android.ui.components.HairlineDivider
import space.pitchstone.android.ui.components.LedgerRow
import space.pitchstone.android.ui.components.PaceBar
import space.pitchstone.android.ui.components.SectionLabel
import space.pitchstone.android.ui.components.StatusDot
import space.pitchstone.android.ui.theme.JetBrainsMono
import space.pitchstone.android.ui.theme.PitchstoneColors

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCapture: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchstoneColors.Background)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PitchstoneColors.Accent)
                }
            }

            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = PitchstoneColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    AccentButton("Retry", onClick = { viewModel.loadLedger() })
                }
            }

            is HomeUiState.Ready -> {
                LedgerContent(
                    summary = state.summary,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
        }

        FloatingActionButton(
            onClick = onNavigateToCapture,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            containerColor = PitchstoneColors.Accent,
            contentColor = PitchstoneColors.Background,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Capture transaction")
        }
    }
}

@Composable
private fun LedgerContent(
    summary: LedgerSummary,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pitchstone",
                color = PitchstoneColors.OnBackground,
                fontFamily = JetBrainsMono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = PitchstoneColors.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionLabel("SPENT · ${summary.monthLabel}")
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatRupees(summary.monthSpent),
            fontFamily = JetBrainsMono,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = PitchstoneColors.OnBackground
        )
        Spacer(Modifier.height(6.dp))
        PaceBar(ratio = summary.monthProgressFraction)
        Spacer(Modifier.height(8.dp))
        StatusDot(label = "agent online")

        Spacer(Modifier.height(32.dp))
        HairlineDivider()
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("BUDGETS")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PitchstoneColors.SurfaceVariant)
                    .clickable(onClick = onNavigateToBudgets)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "edit →",
                    color = PitchstoneColors.Accent,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        summary.categories.forEach { categorySpend ->
            BudgetPaceRow(categorySpend = categorySpend)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        HairlineDivider()
        Spacer(Modifier.height(24.dp))

        SectionLabel("RECENT")
        Spacer(Modifier.height(12.dp))

        if (summary.recentTransactions.isEmpty()) {
            Text(
                text = "No transactions yet. Tap + to capture one.",
                color = PitchstoneColors.OnSurfaceVariant,
                fontSize = 14.sp
            )
        } else {
            summary.recentTransactions.forEachIndexed { index, txn ->
                LedgerRow(
                    recipient = txn.recipient ?: "Unknown",
                    meta = transactionMeta(txn),
                    amount = formatRupeesString(txn.amount),
                    onClick = { onNavigateToDetail(txn.id) }
                )
                if (index < summary.recentTransactions.lastIndex) {
                    HairlineDivider()
                }
            }
        }

        Spacer(Modifier.height(80.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BudgetPaceRow(categorySpend: CategorySpend) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = categorySpend.category.name,
                color = PitchstoneColors.OnBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row {
                Text(
                    text = formatRupees(categorySpend.spent),
                    color = if (categorySpend.isOver) PitchstoneColors.Danger else PitchstoneColors.OnSurfaceVariant,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
                Text(
                    text = " / ${formatRupees(categorySpend.category.monthlyCap)}",
                    color = PitchstoneColors.OnSurfaceVariant,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        PaceBar(ratio = categorySpend.ratio)
    }
}
