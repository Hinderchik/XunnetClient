package dev.xunnet.client.ui.components

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.xunnet.client.core.domain.parser.LinkParser

/**
 * Bottom-sheet dialog for importing a proxy profile.
 * - Pastes from clipboard automatically when opened
 * - Lets user paste/edit a link
 * - Returns the parsed Profile via onImport, or null if cancelled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(
    parser: LinkParser,
    onImport: (Result<dev.xunnet.client.core.domain.model.Profile>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var linkText by remember { mutableStateOf("") }
    var nameOverride by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isParsing by remember { mutableStateOf(false) }

    // Auto-paste from clipboard on first show
    LaunchedEffect(Unit) {
        try {
            val clip = clipboard.primaryClip
            if (clip != null && clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                val text = clip.getItemAt(0).coerceToText(context).toString().trim()
                if (text.isNotEmpty() && text.contains("://")) {
                    linkText = text
                }
            }
        } catch (_: Exception) { }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with gradient accent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Импорт узла",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Link input with paste button
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it; error = null },
                    label = { Text("Ссылка или конфиг") },
                    placeholder = { Text("vless://... / vmess://... / ss://... / trojan://...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = {
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                        else Text("Поддерживаются: vless, vmess, trojan, ss, hysteria(1/2), tuic, ssh, naive, wireguard, xunnet, xuncrypt")
                    }
                )

                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    linkText = clip.getItemAt(0).coerceToText(context).toString().trim()
                                    error = null
                                }
                            } catch (e: Exception) {
                                error = "Не удалось прочитать буфер: ${e.message}"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Вставить")
                    }
                    OutlinedButton(
                        onClick = { /* TODO: QR scanner */ },
                        modifier = Modifier.weight(1f),
                        enabled = false
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("QR-код")
                    }
                }

                // Optional name override
                OutlinedTextField(
                    value = nameOverride,
                    onValueChange = { nameOverride = it },
                    label = { Text("Имя (опционально)") },
                    placeholder = { Text("Оставьте пустым — возьмём из ссылки") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Отмена") }
                    Button(
                        onClick = {
                            isParsing = true
                            error = null
                            val result = parser.parse(linkText)
                            result.fold(
                                onSuccess = { p ->
                                    val finalProfile = if (nameOverride.isNotBlank())
                                        p.copy(name = nameOverride)
                                    else p
                                    onImport(Result.success(finalProfile))
                                },
                                onFailure = {
                                    error = "Не удалось разобрать: ${it.message}"
                                }
                            )
                            isParsing = false
                        },
                        modifier = Modifier.weight(1f),
                        enabled = linkText.isNotBlank() && !isParsing
                    ) {
                        if (isParsing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Импорт")
                        }
                    }
                }
            }
        }
    }
}
