package dev.xunnet.client.core.domain.model

data class Stats(
    val uploadBytes: Long = 0,
    val downloadBytes: Long = 0,
    val uploadSpeed: Long = 0,
    val downloadSpeed: Long = 0,
    val connected: Boolean = false,
    val activeProfileId: String? = null
)
