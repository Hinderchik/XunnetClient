package dev.xunnet.client.core.domain.repository

import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Subscription

interface SubscriptionRepository {
    suspend fun getAll(): List<Subscription>
    suspend fun add(subscription: Subscription): Result<Unit>
    suspend fun update(subscription: Subscription): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun refresh(id: String): Result<List<Profile>>
    suspend fun refreshAll(): Result<Unit>
    suspend fun aggregate(subscriptionIds: List<String>): Result<Subscription>
}
