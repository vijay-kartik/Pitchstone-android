package space.pitchstone.android.presentation.budgets

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.pitchstone.android.domain.model.BudgetCategory
import space.pitchstone.android.presentation.util.formatRupees
import space.pitchstone.android.ui.components.AccentButton
import space.pitchstone.android.ui.components.HairlineDivider
import space.pitchstone.android.ui.components.PaceBar
import space.pitchstone.android.ui.components.ScreenHeader
import space.pitchstone.android.ui.components.SectionLabel
import space.pitchstone.android.ui.theme.JetBrainsMono
import space.pitchstone.android.ui.theme.PitchstoneColors
import space.pitchstone.android.ui.theme.SpaceGrotesk
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    ScreenHeader(title = "Budgets", onBack = onBack)
                    Spacer(Modifier.height(24.dp))

                    SectionLabel("MONTHLY CAPS")
                    Spacer(Modifier.height(16.dp))

                    state.categories.forEachIndexed { index, category ->
                        BudgetCategoryRow(
                            category = category,
                            onDecrement = { viewModel.decrementCap(category) },
                            onIncrement = { viewModel.incrementCap(category) }
                        )
                        if (index < state.categories.lastIndex) {
                            Spacer(Modifier.height(4.dp))
                            HairlineDivider()
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                    AccentButton(
                        text = "+ New category",
                        onClick = { showAddDialog = true }
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
    category: BudgetCategory,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                color = PitchstoneColors.OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease ₹500",
                        tint = PitchstoneColors.OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = formatRupees(category.monthlyCap),
                    color = PitchstoneColors.OnBackground,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase ₹500",
                        tint = PitchstoneColors.Accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        PaceBar(ratio = 0f)
        Spacer(Modifier.height(4.dp))
        Text(
            text = category.keywords.joinToString(", "),
            color = PitchstoneColors.OnSurfaceVariant,
            fontSize = 11.sp
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
