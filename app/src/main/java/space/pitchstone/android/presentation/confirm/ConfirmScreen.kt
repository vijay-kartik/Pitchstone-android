package space.pitchstone.android.presentation.confirm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import space.pitchstone.android.ui.components.AccentButton
import space.pitchstone.android.ui.components.HairlineDivider
import space.pitchstone.android.ui.components.OutlineButton
import space.pitchstone.android.ui.components.ScreenHeader
import space.pitchstone.android.ui.components.SectionLabel
import space.pitchstone.android.ui.theme.JetBrainsMono
import space.pitchstone.android.ui.theme.PitchstoneColors
import space.pitchstone.android.ui.theme.SpaceGrotesk

@Composable
fun ConfirmScreen(
    viewModel: ConfirmViewModel,
    onBackClick: () -> Unit,
    onSavedSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ConfirmUiState.Saved) onSavedSuccess()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchstoneColors.Background)
    ) {
        when (val state = uiState) {
            is ConfirmUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PitchstoneColors.Accent)
                }
            }

            is ConfirmUiState.RawText -> {
                RawTextContent(
                    text = state.text,
                    onClose = onBackClick
                )
            }

            is ConfirmUiState.Extracted -> {
                ExtractedFormContent(
                    initialFields = state.fields,
                    onSave = { viewModel.saveTransaction(it) },
                    errorMessage = null,
                    isSaving = false,
                    onBackClick = onBackClick
                )
            }

            is ConfirmUiState.Saving -> {
                ExtractedFormContent(
                    initialFields = state.fields,
                    onSave = {},
                    errorMessage = null,
                    isSaving = true,
                    onBackClick = onBackClick
                )
            }

            is ConfirmUiState.SaveError -> {
                ExtractedFormContent(
                    initialFields = state.fields,
                    onSave = { viewModel.saveTransaction(it) },
                    errorMessage = state.message,
                    isSaving = false,
                    onBackClick = onBackClick
                )
            }

            else -> {}
        }
    }
}

@Composable
private fun RawTextContent(text: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        ScreenHeader(title = "Agent Reply", onBack = onClose)
        Spacer(Modifier.height(24.dp))

        SelectionContainer {
            Text(
                text = text,
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                color = PitchstoneColors.OnSurfaceVariant,
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(28.dp))
        OutlineButton(text = "Done", onClick = onClose)
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ExtractedFormContent(
    initialFields: ExtractedFields,
    onSave: (ExtractedFields) -> Unit,
    errorMessage: String?,
    isSaving: Boolean,
    onBackClick: () -> Unit
) {
    var amount by remember { mutableStateOf(initialFields.amount) }
    var currency by remember { mutableStateOf(initialFields.currency) }
    var dateTime by remember { mutableStateOf(initialFields.dateTime) }
    var sender by remember { mutableStateOf(initialFields.sender) }
    var recipient by remember { mutableStateOf(initialFields.recipient) }
    var transactionId by remember { mutableStateOf(initialFields.transactionId) }
    var status by remember { mutableStateOf(initialFields.status) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        ScreenHeader(title = "Review before saving", onBack = onBackClick)
        Spacer(Modifier.height(24.dp))

        ConfirmFieldRow("Amount", amount) { amount = it }
        HairlineDivider()
        ConfirmFieldRow("Currency", currency) { currency = it }
        HairlineDivider()
        ConfirmFieldRow("Date & Time", dateTime) { dateTime = it }
        HairlineDivider()
        ConfirmFieldRow("Sender", sender) { sender = it }
        HairlineDivider()
        ConfirmFieldRow("Recipient", recipient) { recipient = it }
        HairlineDivider()
        ConfirmFieldRow("Transaction ID", transactionId) { transactionId = it }
        HairlineDivider()
        ConfirmFieldRow("Status", status) { status = it }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = PitchstoneColors.Danger,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(24.dp))
        AccentButton(
            text = if (isSaving) "Saving…" else "Save transaction",
            onClick = {
                onSave(
                    ExtractedFields(
                        amount = amount,
                        currency = currency,
                        dateTime = dateTime,
                        sender = sender,
                        recipient = recipient,
                        transactionId = transactionId,
                        status = status,
                        rawMap = initialFields.rawMap
                    )
                )
            },
            enabled = !isSaving
        )
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ConfirmFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        SectionLabel(label)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
                color = PitchstoneColors.OnBackground
            ),
            cursorBrush = SolidColor(PitchstoneColors.Accent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
