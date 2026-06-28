package dev.xunnet.client.core.domain.model

data class Profile(
    val id: String,
    val name: String,
    val protocol: String,
    val address: String,
    val port: Int,
    val paramsJson: String = "{}",
    val encryption: String? = null,
    val params: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val priority: Int = 5,
    val enabled: Boolean = true,
    val source: String? = null,
    val latencyMs: Long? = null,
    val latency: Long? = null,
    val speed: Long? = null
)
