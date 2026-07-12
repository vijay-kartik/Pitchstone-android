package space.pitchstone.android.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.pitchstone.android.R
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
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCapture: () -> Unit,
    onNavigateToAsk: () -> Unit,
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
                    onNavigateToAsk = onNavigateToAsk,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
        }

        // Pill FAB — "+ Capture" with accent glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .navigationBarsPadding()
                .clip(RoundedCornerShape(100.dp))
                .background(PitchstoneColors.Accent)
                .clickable(onClick = onNavigateToCapture)
                .padding(horizontal = 24.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ Capture",
                color = PitchstoneColors.Background,
                fontFamily = JetBrainsMono,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LedgerContent(
    summary: LedgerSummary,
    onNavigateToAsk: () -> Unit,
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

        // Header: title + ASK / CFG chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.pitchstone_wordmark),
                contentDescription = "Pitchstone",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(17.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoChip(label = "ASK", onClick = onNavigateToAsk)
                MonoChip(label = "CFG", onClick = onNavigateToSettings)
            }
        }

        // SPENT hero
        Spacer(Modifier.height(26.dp))
        SectionLabel("SPENT · ${summary.monthLabel}")
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatRupees(summary.monthSpent),
            fontFamily = JetBrainsMono,
            fontSize = 42.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1).sp,
            color = PitchstoneColors.OnBackground
        )
        Spacer(Modifier.height(10.dp))
        StatusDot(label = "agent online")

        // BUDGETS section
        Spacer(Modifier.height(28.dp))
        val monthPct = (summary.monthProgressFraction * 100).roundToInt()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("BUDGETS · $monthPct% OF MONTH GONE")
            Text(
                text = "edit →",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = PitchstoneColors.OnSurfaceVariant,
                modifier = Modifier.clickable(onClick = onNavigateToBudgets)
            )
        }
        Spacer(Modifier.height(8.dp))

        summary.categories.forEach { categorySpend ->
            BudgetPaceRow(
                categorySpend = categorySpend,
                tickRatio = summary.monthProgressFraction
            )
            Spacer(Modifier.height(11.dp))
        }

        if (summary.categories.isNotEmpty()) {
            Text(
                text = "│ = where the month is — a bar past the tick is burning faster than time",
                color = Color(0xFF3D444E),
                fontFamily = JetBrainsMono,
                fontSize = 9.sp
            )
        }

        // RECENT section
        Spacer(Modifier.height(20.dp))
        HairlineDivider()
        Spacer(Modifier.height(20.dp))
        SectionLabel("RECENT")
        Spacer(Modifier.height(10.dp))

        if (summary.recentTransactions.isEmpty()) {
            Text(
                text = "No transactions yet — tap + Capture to add one.",
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

        Spacer(Modifier.height(90.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun MonoChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMono,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = PitchstoneColors.OnSurfaceVariant
        )
    }
}

@Composable
private fun BudgetPaceRow(categorySpend: CategorySpend, tickRatio: Float) {
    val rightColor = if (categorySpend.isOver) PitchstoneColors.Danger else PitchstoneColors.OnSurfaceVariant
    val rightLabel = if (categorySpend.isOver) {
        "${formatRupees(-categorySpend.remaining)} over"
    } else {
        "${formatRupees(categorySpend.remaining)} left"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categorySpend.category.name,
                color = PitchstoneColors.TextSecondary,
                fontSize = 12.5.sp
            )
            Text(
                text = rightLabel,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = rightColor
            )
        }
        Spacer(Modifier.height(6.dp))
        PaceBar(ratio = categorySpend.ratio, tickRatio = tickRatio)
    }
}
