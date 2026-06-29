package dev.xunnet.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xunnet.client.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Spacer(Modifier.weight(1f))

        // Single hero status — minimal
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(statusGradient(uiState.isConnected, uiState.isConnecting)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    uiState.isConnecting -> "Подключение"
                    uiState.isConnected -> "Защищено"
                    else -> "Не подключено"
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (uiState.activeProfile?.name != null) {
                Text(
                    text = uiState.activeProfile?.name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Single primary action
        Button(
            onClick = {
                if (uiState.isConnected) viewModel.disconnect()
                else uiState.activeProfile?.let { viewModel.connect(it) }
            },
            enabled = !uiState.isConnecting && (uiState.isConnected || uiState.activeProfile != null),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isConnected) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = when {
                    uiState.isConnecting -> "..."
                    uiState.isConnected -> "Отключить"
                    else -> "Подключиться"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Minimal speed line — no icons, no labels, just two numbers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "↓ ${uiState.downloadSpeed}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "↑ ${uiState.uploadSpeed}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun statusGradient(connected: Boolean, connecting: Boolean): Brush {
    return when {
        connecting -> Brush.linearGradient(
            colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
        )
        connected -> Brush.linearGradient(
            colors = listOf(Color(0xFF22C55E), Color(0xFF10B981))
        )
        else -> Brush.linearGradient(
            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
        )
    }
}
