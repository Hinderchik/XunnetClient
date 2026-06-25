package dev.xunnet.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FederationScreen() {
    val panels = listOf(
        Triple("US Panel", "panel-us.xunnet.dev", "🟢 Online — 45 серверов"),
        Triple("EU Panel", "panel-eu.xunnet.dev", "🟢 Online — 32 сервера"),
        Triple("ASIA Panel", "panel-asia.xunnet.dev", "🔴 Offline — timeout")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Федерация",
            style = MaterialTheme.typography.headlineMedium
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(panels) { (name, host, status) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                        Text(host, style = MaterialTheme.typography.bodyMedium)
                        Text(status, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
