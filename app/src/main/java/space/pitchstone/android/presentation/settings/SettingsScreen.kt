package space.pitchstone.android.presentation.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.pitchstone.android.domain.model.AgentProvider
import space.pitchstone.android.ui.components.MonoPill
import space.pitchstone.android.ui.components.ScreenHeader
import space.pitchstone.android.ui.theme.JetBrainsMono
import space.pitchstone.android.ui.theme.PitchstoneColors

// Vision-capable chat models only — Capture always sends screenshots as image
// content parts, so a text-only model would silently ignore every attachment.
private val OPENAI_MODEL_OPTIONS = listOf(
    "gpt-5.4-mini",
    "gpt-5.4",
    "gpt-4o-mini",
    "gpt-4o",
    "gpt-4.1-mini",
    "gpt-4.1",
    "gpt-4.1-nano"
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val provider by viewModel.provider.collectAsState()
    val openAiApiKey by viewModel.openAiApiKey.collectAsState()
    val openAiModel by viewModel.openAiModel.collectAsState()
    val autoFallback by viewModel.autoFallback.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchstoneColors.Background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        ScreenHeader(
            title = "Agent connection",
            subtitle = "who answers when you run the agent",
            onBack = onBackClick,
            trailingContent = {
                MonoPill(
                    text = if (provider == AgentProvider.GATEWAY) "GATEWAY" else "OPENAI",
                    color = PitchstoneColors.Accent
                )
            }
        )
        Spacer(Modifier.height(18.dp))

        // Provider: OpenClaw gateway
        ProviderCard(
            selected = provider == AgentProvider.GATEWAY,
            onSelect = { viewModel.selectProvider(AgentProvider.GATEWAY) },
            title = "OpenClaw gateway",
            meta = "local model · private · free"
        ) {
            FieldBlock(label = "BASE URL") {
                EditableFieldBox(
                    value = baseUrl,
                    placeholder = "https://host.tailnet.ts.net",
                    onValueChange = viewModel::updateBaseUrl
                )
            }
            Spacer(Modifier.height(12.dp))
            FieldBlock(label = "API KEY") {
                SecretFieldBox(
                    value = apiKey,
                    placeholder = "Enter your gateway API key",
                    onValueChange = viewModel::updateApiKey
                )
            }
            Spacer(Modifier.height(12.dp))
            TestConnectionSlot(
                status = connectionStatus,
                onTest = viewModel::testConnection
            )
        }

        Spacer(Modifier.height(16.dp))

        // Provider: OpenAI API
        ProviderCard(
            selected = provider == AgentProvider.OPENAI,
            onSelect = { viewModel.selectProvider(AgentProvider.OPENAI) },
            title = "OpenAI API",
            meta = "cloud · ${openAiModel.ifBlank { "gpt-4o" }} · paid per call"
        ) {
            FieldBlock(label = "API KEY") {
                SecretFieldBox(
                    value = openAiApiKey,
                    placeholder = "sk-…",
                    onValueChange = viewModel::updateOpenAiApiKey
                )
            }
            Spacer(Modifier.height(12.dp))
            FieldBlock(label = "MODEL") {
                ModelSelectorBox(
                    selectedModel = openAiModel,
                    onModelSelected = viewModel::updateOpenAiModel
                )
            }
            if (openAiApiKey.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "✓ key added",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = PitchstoneColors.Accent
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Auto-fallback toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
                .clickable { viewModel.setAutoFallback(!autoFallback) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-fallback to OpenAI",
                    color = PitchstoneColors.OnBackground,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "If the gateway doesn't respond in 15s, retry the same request on OpenAI.",
                    color = PitchstoneColors.TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            PillToggle(checked = autoFallback)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Screenshots sent to OpenAI leave your device — fallback runs only after you've added a key here.",
            color = PitchstoneColors.TextMuted,
            fontSize = 11.5.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ProviderCard(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    meta: String,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) {
        PitchstoneColors.Accent.copy(alpha = 0.4f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (selected) PitchstoneColors.Accent.copy(alpha = 0.05f) else Color.Transparent
                )
                .clickable(onClick = onSelect)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioDot(selected = selected)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) PitchstoneColors.OnBackground else PitchstoneColors.TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = if (selected) PitchstoneColors.OnSurfaceVariant else PitchstoneColors.TextMuted
                )
            }
            Text(
                text = if (selected) "active" else "standby",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = if (selected) PitchstoneColors.Accent else PitchstoneColors.TextMuted
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .border(
                1.5.dp,
                if (selected) PitchstoneColors.Accent else Color.White.copy(alpha = 0.3f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(PitchstoneColors.Accent)
            )
        }
    }
}

@Composable
private fun FieldBlock(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            fontFamily = JetBrainsMono,
            fontSize = 9.5.sp,
            letterSpacing = 1.5.sp,
            color = PitchstoneColors.TextMuted
        )
        Spacer(Modifier.height(7.dp))
        content()
    }
}

@Composable
private fun FieldBoxContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PitchstoneColors.InputField, RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Composable
private fun EditableFieldBox(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    FieldBoxContainer {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 11.5.sp,
                color = PitchstoneColors.OnBackground
            ),
            cursorBrush = SolidColor(PitchstoneColors.Accent),
            singleLine = true,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.5.sp,
                            color = PitchstoneColors.TextMuted
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Masked "3d68 ···· ···· b619" display; tap "reveal" to show and edit the key in place. */
@Composable
private fun SecretFieldBox(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    var revealed by remember { mutableStateOf(value.isEmpty()) }

    if (revealed) {
        FieldBoxContainer {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 11.5.sp,
                        color = PitchstoneColors.OnBackground
                    ),
                    cursorBrush = SolidColor(PitchstoneColors.Accent),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.5.sp,
                                    color = PitchstoneColors.TextMuted
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                if (value.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "hide",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.5.sp,
                        color = PitchstoneColors.TextMuted,
                        modifier = Modifier.clickable { revealed = false }
                    )
                }
            }
        }
    } else {
        FieldBoxContainer {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = maskKey(value),
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    color = PitchstoneColors.OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "reveal",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    color = PitchstoneColors.TextMuted,
                    modifier = Modifier.clickable { revealed = true }
                )
            }
        }
    }
}

private fun maskKey(key: String): String = when {
    key.isBlank() -> "not set"
    key.length <= 8 -> "····"
    else -> "${key.take(4)} ···· ···· ${key.takeLast(4)}"
}

@Composable
private fun ModelSelectorBox(
    selectedModel: String,
    onModelSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PitchstoneColors.InputField, RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 13.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedModel.ifBlank { OPENAI_MODEL_OPTIONS.first() },
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    color = PitchstoneColors.OnBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "▾",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    color = PitchstoneColors.TextMuted
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OPENAI_MODEL_OPTIONS.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = model,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = if (model == selectedModel) {
                                PitchstoneColors.Accent
                            } else {
                                PitchstoneColors.OnBackground
                            }
                        )
                    },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Doc rule: outlined "Test connection" button; success swaps a mono status line
 * into the same slot — no toast. Tap the result line to test again.
 */
@Composable
private fun TestConnectionSlot(
    status: SettingsViewModel.ConnectionStatus,
    onTest: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    when (status) {
        is SettingsViewModel.ConnectionStatus.Success -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(PitchstoneColors.Accent.copy(alpha = 0.08f))
                    .clickable(onClick = onTest)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "200 OK · ${status.elapsedMs}ms · /healthz",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.5.sp,
                    color = PitchstoneColors.Accent
                )
            }
        }

        is SettingsViewModel.ConnectionStatus.Failure -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(PitchstoneColors.Danger.copy(alpha = 0.08f))
                    .clickable(onClick = onTest)
                    .padding(vertical = 13.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status.error,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    color = PitchstoneColors.Danger,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        else -> {
            val checking = status is SettingsViewModel.ConnectionStatus.Checking
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.dp, PitchstoneColors.Accent.copy(alpha = 0.4f), shape)
                    .clickable(enabled = !checking, onClick = onTest)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (checking) "testing…" else "Test connection",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PitchstoneColors.Accent
                )
            }
        }
    }
}

/** 46×27 pill toggle per the doc — accent track when on, dark knob. */
@Composable
private fun PillToggle(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 27.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(
                if (checked) PitchstoneColors.Accent else Color.White.copy(alpha = 0.12f)
            ),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(21.dp)
                .clip(CircleShape)
                .background(if (checked) PitchstoneColors.Background else PitchstoneColors.TextSecondary)
        )
    }
}
