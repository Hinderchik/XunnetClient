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
fun ProxiesScreen() {
    val proxies = listOf(
        "US_VIP_01" to "VLESS+REALITY — 23ms",
        "DE_02" to "VMess+WS — 45ms",
        "JP_03" to "Trojan — 67ms"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Узлы",
            style = MaterialTheme.typography.headlineMedium
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(proxies) { (name, info) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                        Text(info, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
