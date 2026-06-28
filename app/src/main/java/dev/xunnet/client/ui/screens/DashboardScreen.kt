package dev.xunnet.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xunnet.client.core.domain.model.Profile
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero status card
        HeroStatusCard(
            isConnected = uiState.isConnected,
            isConnecting = uiState.isConnecting,
            activeProfileName = uiState.activeProfile?.name
        )

        // Big connect button
        BigActionButton(
            isConnected = uiState.isConnected,
            isConnecting = uiState.isConnecting,
            enabled = uiState.isConnected || uiState.activeProfile != null,
            onClick = {
                if (uiState.isConnected) viewModel.disconnect()
                else uiState.activeProfile?.let { viewModel.connect(it) }
            }
        )

        // Speed + totals grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CloudDownload,
                label = "Загрузка",
                value = uiState.downloadSpeed,
                accent = MaterialTheme.colorScheme.primary
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CloudUpload,
                label = "Отдача",
                value = uiState.uploadSpeed,
                accent = MaterialTheme.colorScheme.tertiary
            )
        }

        // Latency badge if active profile has it
        uiState.activeProfile?.latencyMs?.let { lat ->
            LatencyBadge(latencyMs = lat)
        }

        // Empty state hint
        AnimatedVisibility(visible = uiState.profiles.isEmpty()) {
            EmptyStateHint()
        }
    }
}

@Composable
private fun HeroStatusCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    activeProfileName: String?
) {
    val gradient = when {
        isConnecting -> Brush.linearGradient(
            colors = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
        )
        isConnected -> Brush.linearGradient(
            colors = listOf(Color(0xFF22C55E), Color(0xFF10B981))
        )
        else -> Brush.linearGradient(
            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PulseIndicator(active = isConnected || isConnecting)
                    Text(
                        text = when {
                            isConnecting -> "Подключение…"
                            isConnected -> "Защищено"
                            else -> "Не подключено"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(
                        text = activeProfileName ?: "Выберите узел",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isConnected) "VPN активен" else "Готов к подключению",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseIndicator(active: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.5f,
        animationSpec = tween(800),
        label = "pulse"
    )
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = alpha))
    )
}

@Composable
private fun BigActionButton(
    isConnected: Boolean,
    isConnecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
            Spacer(Modifier.width(12.dp))
            Text("Подключение…", style = MaterialTheme.typography.titleMedium)
        } else {
            Icon(
                imageVector = if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.Bolt,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isConnected) "Отключить" else "Подключиться",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LatencyBadge(latencyMs: Long) {
    val color = when {
        latencyMs < 50 -> Color(0xFF22C55E)
        latencyMs < 150 -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Speed, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text("Пинг: $latencyMs ms", style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun EmptyStateHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("У вас пока нет узлов", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Добавьте через раздел «Узлы» — вставьте ссылку из буфера обмена или QR-код",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
