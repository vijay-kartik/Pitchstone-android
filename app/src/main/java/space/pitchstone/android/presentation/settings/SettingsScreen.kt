package space.pitchstone.android.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.pitchstone.android.ui.theme.ErrorRed
import space.pitchstone.android.ui.theme.InfoBlue
import space.pitchstone.android.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "API Gateway Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { viewModel.updateBaseUrl(it) },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://kartiks-mac-mini-7.tailafb282.ts.net") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { viewModel.updateApiKey(it) },
                        label = { Text("Gateway API Key") },
                        placeholder = { Text("Enter your secure API key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.testConnection() },
                        enabled = connectionStatus !is SettingsViewModel.ConnectionStatus.Checking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (connectionStatus is SettingsViewModel.ConnectionStatus.Checking) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Test Connection")
                        }
                    }
                }
            }

            // Connection Status
            AnimatedVisibility(
                visible = connectionStatus !is SettingsViewModel.ConnectionStatus.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (connectionStatus) {
                            is SettingsViewModel.ConnectionStatus.Success -> SuccessGreen.copy(alpha = 0.12f)
                            is SettingsViewModel.ConnectionStatus.Failure -> ErrorRed.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (connectionStatus) {
                            is SettingsViewModel.ConnectionStatus.Success -> Icons.Default.CheckCircle
                            is SettingsViewModel.ConnectionStatus.Failure -> Icons.Default.Warning
                            else -> Icons.Default.Info
                        }
                        val tint = when (connectionStatus) {
                            is SettingsViewModel.ConnectionStatus.Success -> SuccessGreen
                            is SettingsViewModel.ConnectionStatus.Failure -> ErrorRed
                            else -> InfoBlue
                        }
                        val message = when (val status = connectionStatus) {
                            is SettingsViewModel.ConnectionStatus.Success -> "Connection successful! Gateway is online."
                            is SettingsViewModel.ConnectionStatus.Failure -> status.error
                            else -> "Testing gateway connectivity..."
                        }

                        Icon(imageVector = icon, contentDescription = null, tint = tint)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Troubleshooting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Troubleshooting & Tailscale",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "• Tailscale Connection Required: The API Gateway runs on the developer's tailnet. Make sure the official Tailscale Android app is running and connected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "• DNS Issues: If the hostname fails to resolve, try using the tailnet IP directly (e.g., https://100.81.125.16). Note that TLS hostname verification will fail with a raw IP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
