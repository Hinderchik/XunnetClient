package dev.xunnet.client.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.xunnet.client.core.data.local.entity.FederatedPanelEntity
import dev.xunnet.client.core.data.local.entity.ProfileEntity
import dev.xunnet.client.core.data.local.entity.SubscriptionEntity

@Database(
    entities = [ProfileEntity::class, SubscriptionEntity::class, FederatedPanelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun federatedPanelDao(): FederatedPanelDao
}
