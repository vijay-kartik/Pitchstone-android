package space.pitchstone.android.presentation.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.pitchstone.android.ui.components.HairlineDivider
import space.pitchstone.android.ui.components.OutlineButton
import space.pitchstone.android.ui.components.ScreenHeader
import space.pitchstone.android.ui.components.SectionLabel
import space.pitchstone.android.ui.components.StatusDot
import space.pitchstone.android.ui.theme.PitchstoneColors
import space.pitchstone.android.ui.theme.SpaceGrotesk

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchstoneColors.Background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        ScreenHeader(title = "Gateway", onBack = onBackClick)
        Spacer(Modifier.height(24.dp))

        SectionLabel("CONNECTION")
        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PitchstoneColors.Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsFieldRow(
                    label = "Base URL",
                    value = baseUrl,
                    placeholder = "https://host.tailnet.ts.net",
                    onValueChange = { viewModel.updateBaseUrl(it) }
                )
                HairlineDivider()
                SettingsFieldRow(
                    label = "API Key",
                    value = apiKey,
                    placeholder = "Enter your secure API key",
                    onValueChange = { viewModel.updateApiKey(it) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlineButton(
            text = when (connectionStatus) {
                is SettingsViewModel.ConnectionStatus.Checking -> "Testing…"
                else -> "Test connection"
            },
            onClick = { viewModel.testConnection() },
            accent = true,
            enabled = connectionStatus !is SettingsViewModel.ConnectionStatus.Checking
        )

        if (connectionStatus is SettingsViewModel.ConnectionStatus.Checking) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(
                color = PitchstoneColors.Accent,
                modifier = Modifier.size(20.dp)
            )
        }

        if (connectionStatus !is SettingsViewModel.ConnectionStatus.Idle &&
            connectionStatus !is SettingsViewModel.ConnectionStatus.Checking
        ) {
            Spacer(Modifier.height(12.dp))
            val (label, color) = when (val status = connectionStatus) {
                is SettingsViewModel.ConnectionStatus.Success ->
                    "Gateway online" to PitchstoneColors.Accent
                is SettingsViewModel.ConnectionStatus.Failure ->
                    (status.error) to PitchstoneColors.Danger
                else -> "" to PitchstoneColors.OnSurfaceVariant
            }
            if (label.isNotBlank()) {
                StatusDot(label = label, color = color)
            }
        }

        Spacer(Modifier.height(32.dp))
        HairlineDivider()
        Spacer(Modifier.height(24.dp))

        SectionLabel("TAILSCALE TIPS")
        Spacer(Modifier.height(12.dp))

        listOf(
            "Tailscale must be running and connected before making requests.",
            "If the hostname fails to resolve, try the tailnet IP directly (TLS verification will fail with a raw IP).",
            "Use the Tailscale Android app — not a VPN profile — for DNS to work correctly."
        ).forEachIndexed { index, tip ->
            Text(
                text = "· $tip",
                color = PitchstoneColors.OnSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            if (index < 2) Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SettingsFieldRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = label,
            color = PitchstoneColors.OnSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = SpaceGrotesk,
                fontSize = 14.sp,
                color = PitchstoneColors.OnBackground
            ),
            cursorBrush = SolidColor(PitchstoneColors.Accent),
            singleLine = true,
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = PitchstoneColors.OnSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
