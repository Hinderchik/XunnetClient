package dev.xunnet.client.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "federated_panels")
data class FederatedPanelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val host: String,
    val apiKey: String,
    val role: String,
    val mode: String,
    val status: String,
    val lastSync: Long?,
    val serversCount: Int,
    val tags: String,
    val enabled: Boolean,
    val createdAt: Long
)
