package dev.xunnet.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.parser.XunnetLinkParser
import dev.xunnet.client.ui.components.ImportDialog
import dev.xunnet.client.ui.viewmodel.ProxiesViewModel

@Composable
fun ProxiesScreen(
    viewModel: ProxiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showImport by remember { mutableStateOf(false) }
    var snackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbar) {
        snackbar?.let { snackbarHostState.showSnackbar(it); snackbar = null }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImport = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Импорт")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.proxies.isEmpty()) {
                EmptyProxiesState(onImportClick = { showImport = true })
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, bottom = 96.dp)
                ) {
                    items(items = uiState.proxies, key = { p -> p.id }) { profile ->
                        MinimalProfileRow(
                            profile = profile,
                            isActive = profile.id == uiState.activeProfileId,
                            onClick = { viewModel.connect(it) },
                            onLongPress = { viewModel.delete(it.id) }
                        )
                    }
                }
            }
        }
    }

    if (showImport) {
        ImportDialog(
            parser = XunnetLinkParser(),
            onImport = { result ->
                showImport = false
                result.fold(
                    onSuccess = { profile ->
                        viewModel.add(profile)
                        snackbar = "Добавлено: ${profile.name}"
                    },
                    onFailure = { snackbar = "Ошибка: ${it.message}" }
                )
            },
            onDismiss = { showImport = false }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MinimalProfileRow(
    profile: Profile,
    isActive: Boolean,
    onClick: (Profile) -> Unit,
    onLongPress: (Profile) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(profile) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active dot — only visible when active
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else {
            Spacer(Modifier.size(10.dp))
        }

        Text(
            text = profile.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        profile.latencyMs?.let { lat ->
            Text(
                text = "$lat",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    lat < 100 -> Color(0xFF22C55E)
                    lat < 300 -> Color(0xFFFBBF24)
                    else -> Color(0xFFEF4444)
                }
            )
        }
    }
}

@Composable
private fun EmptyProxiesState(onImportClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                "Скопируй ссылку на узел и нажми +",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
