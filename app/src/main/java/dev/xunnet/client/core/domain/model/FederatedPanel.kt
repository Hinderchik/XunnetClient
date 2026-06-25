package dev.xunnet.client.core.domain.model

data class FederatedPanel(
    val id: String,
    val name: String,
    val host: String,
    val apiKey: String,
    val role: String = "peer",
    val mode: String = "pull",
    val status: String = "pending",
    val lastSync: Long? = null,
    val serversCount: Int = 0,
    val tags: List<String> = emptyList(),
    val enabled: Boolean = true,
    val syncInterval: Int = 300
)
