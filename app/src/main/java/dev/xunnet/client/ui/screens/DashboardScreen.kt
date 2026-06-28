package dev.xunnet.client.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xunnet.client.R
import dev.xunnet.client.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Xunnet",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        StatusCard(
            isConnected = uiState.isConnected,
            isConnecting = uiState.isConnecting,
            activeProfileName = uiState.activeProfile?.name
        )

        Button(
            onClick = {
                if (uiState.isConnected) viewModel.disconnect()
                else uiState.activeProfile?.let { viewModel.connect(it) }
            },
            enabled = !uiState.isConnecting && (uiState.isConnected || uiState.activeProfile != null),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = if (uiState.isConnected) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else ButtonDefaults.buttonColors()
        ) {
            if (uiState.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                Text(
                    text = if (uiState.isConnected) stringResource(R.string.disconnect)
                    else stringResource(R.string.dashboard_connect),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        StatsCard(
            downloadSpeed = uiState.downloadSpeed,
            uploadSpeed = uiState.uploadSpeed,
            totalDown = uiState.totalDownload,
            totalUp = uiState.totalUpload
        )

        if (uiState.profiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.proxies_empty),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    activeProfileName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnecting -> MaterialTheme.colorScheme.tertiary
                            isConnected -> Color(0xFF22C55E)
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isConnecting -> "Подключение…"
                        isConnected -> "Подключено"
                        else -> "Отключено"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isConnected) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface
                )
                if (activeProfileName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeProfileName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    downloadSpeed: String,
    uploadSpeed: String,
    totalDown: String,
    totalUp: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatRow("📥", stringResource(R.string.dashboard_download), downloadSpeed)
            StatRow("📤", stringResource(R.string.dashboard_upload), uploadSpeed)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Всего: ↑ $totalUp  ↓ $totalDown",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatRow(emoji: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$emoji $label", style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
