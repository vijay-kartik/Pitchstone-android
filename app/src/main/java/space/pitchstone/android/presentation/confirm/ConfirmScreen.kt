package space.pitchstone.android.presentation.confirm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import space.pitchstone.android.ui.theme.ErrorRed
import space.pitchstone.android.ui.theme.InfoBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmScreen(
    viewModel: ConfirmViewModel,
    onBackClick: () -> Unit,
    onSavedSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ConfirmUiState.Saved) {
            onSavedSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState) {
                            is ConfirmUiState.RawText -> "Agent Response"
                            else -> "Confirm Transaction"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ConfirmUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ConfirmUiState.RawText -> {
                    RawTextContent(text = state.text, onClose = onBackClick, modifier = Modifier.fillMaxSize().padding(16.dp))
                }
                is ConfirmUiState.Extracted -> {
                    ExtractedFormContent(initialFields = state.fields, onSave = { viewModel.saveTransaction(it) }, errorMessage = null, isSaving = false, modifier = Modifier.fillMaxSize().padding(16.dp))
                }
                is ConfirmUiState.Saving -> {
                    ExtractedFormContent(initialFields = state.fields, onSave = {}, errorMessage = null, isSaving = true, modifier = Modifier.fillMaxSize().padding(16.dp))
                }
                is ConfirmUiState.SaveError -> {
                    ExtractedFormContent(initialFields = state.fields, onSave = { viewModel.saveTransaction(it) }, errorMessage = state.message, isSaving = false, modifier = Modifier.fillMaxSize().padding(16.dp))
                }
                else -> {}
            }
        }
    }
}

@Composable
fun RawTextContent(text: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = InfoBlue.copy(alpha = 0.12f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "The agent returned text instead of structured transaction data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done")
        }
    }
}

@Composable
fun ExtractedFormContent(
    initialFields: ExtractedFields,
    onSave: (ExtractedFields) -> Unit,
    errorMessage: String?,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf(initialFields.amount) }
    var currency by remember { mutableStateOf(initialFields.currency) }
    var dateTime by remember { mutableStateOf(initialFields.dateTime) }
    var sender by remember { mutableStateOf(initialFields.sender) }
    var recipient by remember { mutableStateOf(initialFields.recipient) }
    var transactionId by remember { mutableStateOf(initialFields.transactionId) }
    var status by remember { mutableStateOf(initialFields.status) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Review extracted details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dateTime, onValueChange = { dateTime = it }, label = { Text("Date & Time") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sender, onValueChange = { sender = it }, label = { Text("Sender") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = transactionId, onValueChange = { transactionId = it }, label = { Text("Transaction/Reference ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }

        // Error
        AnimatedVisibility(visible = errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                        Spacer(Modifier.width(12.dp))
                        Text(text = errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                onSave(ExtractedFields(amount = amount, currency = currency, dateTime = dateTime, sender = sender, recipient = recipient, transactionId = transactionId, status = status, rawMap = initialFields.rawMap))
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save to Database", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
