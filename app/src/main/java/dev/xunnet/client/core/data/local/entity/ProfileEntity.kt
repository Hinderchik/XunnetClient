package dev.xunnet.client.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val address: String,
    val port: Int,
    val paramsJson: String,
    val tags: String,
    val priority: Int,
    val enabled: Boolean,
    val source: String?,
    val latencyMs: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
