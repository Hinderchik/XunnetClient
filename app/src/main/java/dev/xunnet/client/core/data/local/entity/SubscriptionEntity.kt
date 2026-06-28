package dev.xunnet.client.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val format: String,
    val updateInterval: Int,
    val enabled: Boolean,
    val tags: String,
    val lastUpdate: Long?,
    val serverCount: Int,
    val createdAt: Long
)
