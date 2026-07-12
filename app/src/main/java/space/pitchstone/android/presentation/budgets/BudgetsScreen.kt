package space.pitchstone.android.presentation.budgets

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.pitchstone.android.domain.model.CategorySpend
import space.pitchstone.android.presentation.util.currentMonthYearLabel
import space.pitchstone.android.presentation.util.formatRupees
import space.pitchstone.android.ui.components.HairlineDivider
import space.pitchstone.android.ui.components.PaceBar
import space.pitchstone.android.ui.components.ScreenHeader
import space.pitchstone.android.ui.theme.JetBrainsMono
import space.pitchstone.android.ui.theme.PitchstoneColors
import space.pitchstone.android.ui.theme.SpaceGrotesk

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchstoneColors.Background)
    ) {
        when (val state = uiState) {
            is BudgetsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PitchstoneColors.Accent)
                }
            }

            is BudgetsUiState.Ready -> {
                val totalCap = state.categories.sumOf { it.category.monthlyCap }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    ScreenHeader(
                        title = "Categories & budgets",
                        subtitle = "caps reset on the 1st · ${currentMonthYearLabel()}",
                        onBack = onBack,
                        trailingContent = {
                            Text(
                                text = "${formatRupees(totalCap)} cap",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = PitchstoneColors.OnSurfaceVariant
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))

                    state.categories.forEachIndexed { index, categorySpend ->
                        BudgetCategoryRow(
                            categorySpend = categorySpend,
                            tickRatio = state.monthProgressFraction,
                            onDecrement = { viewModel.decrementCap(categorySpend.category) },
                            onIncrement = { viewModel.incrementCap(categorySpend.category) }
                        )
                        if (index < state.categories.lastIndex) {
                            HairlineDivider()
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Dashed "+ New category" button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                PitchstoneColors.OnSurfaceVariant.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showAddDialog = true }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ New category",
                            color = PitchstoneColors.OnSurfaceVariant,
                            fontSize = 13.5.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "the agent tags each saved transaction with one of these — untagged spends land in \"everything else\".",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = PitchstoneColors.OnSurfaceVariant.copy(alpha = 0.5f),
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onConfirm = { name, cap, keywords ->
                viewModel.addCategory(name, cap, keywords)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun BudgetCategoryRow(
    categorySpend: CategorySpend,
    tickRatio: Float,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val category = categorySpend.category
    val usedPct = if (category.monthlyCap > 0) {
        (categorySpend.spent * 100) / category.monthlyCap
    } else 0
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    color = PitchstoneColors.OnBackground,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${formatRupees(categorySpend.spent)} spent · $usedPct% used",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = if (categorySpend.isOver) PitchstoneColors.Danger else PitchstoneColors.TextMuted
                )
            }
            Spacer(Modifier.width(12.dp))

            CapStepperButton(glyph = "−", contentDescription = "Decrease ₹500", onClick = onDecrement)
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatRupees(category.monthlyCap),
                color = PitchstoneColors.OnBackground,
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(80.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.width(6.dp))
            CapStepperButton(glyph = "+", contentDescription = "Increase ₹500", onClick = onIncrement)
        }
        Spacer(Modifier.height(12.dp))
        PaceBar(ratio = categorySpend.ratio, tickRatio = tickRatio)
    }
}

@Composable
private fun CapStepperButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, PitchstoneColors.Outline, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = PitchstoneColors.TextSecondary,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onConfirm: (name: String, cap: Int, keywords: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var capText by remember { mutableStateOf("") }
    var keywordsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PitchstoneColors.Surface,
        title = {
            Text("New category", color = PitchstoneColors.OnBackground, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogField(label = "Name", value = name, onValueChange = { name = it })
                DialogField(
                    label = "Monthly cap (₹)",
                    value = capText,
                    onValueChange = { capText = it }
                )
                DialogField(
                    label = "Keywords (comma-separated)",
                    value = keywordsText,
                    onValueChange = { keywordsText = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cap = capText.toIntOrNull() ?: 0
                    val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onConfirm(name, cap, keywords)
                }
            ) {
                Text("Add", color = PitchstoneColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PitchstoneColors.OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun DialogField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = PitchstoneColors.OnSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = SpaceGrotesk,
                fontSize = 14.sp,
                color = PitchstoneColors.OnBackground
            ),
            cursorBrush = SolidColor(PitchstoneColors.Accent),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PitchstoneColors.SurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
