package dev.xunnet.client.core.domain.model

data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val format: String = "xunnet",
    val updateInterval: Int = 3600,
    val enabled: Boolean = true,
    val tags: List<String> = emptyList(),
    val serverCount: Int = 0,
    val servers: List<Profile> = emptyList(),
    val lastUpdate: Long? = null
)
