package dev.xunnet.client.core.data.repository

import dev.xunnet.client.core.data.local.ProfileDao
import dev.xunnet.client.core.data.local.SubscriptionDao
import dev.xunnet.client.core.data.local.entity.SubscriptionEntity
import dev.xunnet.client.core.data.remote.SubscriptionApi
import dev.xunnet.client.core.data.remote.SubscriptionFetcher
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Subscription
import dev.xunnet.client.core.domain.parser.LinkParser
import dev.xunnet.client.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody
import java.util.UUID

class SubscriptionRepositoryImpl(
    private val dao: SubscriptionDao,
    private val profileDao: ProfileDao,
    private val api: SubscriptionApi,
    private val parser: LinkParser,
    private val fetcher: SubscriptionFetcher = SubscriptionFetcher(parser)
) : SubscriptionRepository {

    override fun observeAll(): Flow<List<Subscription>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun getAll(): List<Subscription> = dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Subscription? = dao.getById(id)?.toDomain()

    override suspend fun add(subscription: Subscription): Result<Unit> = runCatching {
        dao.insert(subscription.toEntity())
    }

    override suspend fun update(subscription: Subscription): Result<Unit> = runCatching {
        dao.update(subscription.toEntity())
    }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        dao.deleteById(id)
        profileDao.deleteBySource(id)
    }

    override suspend fun refresh(id: String): Result<List<Profile>> = runCatching {
        val sub = dao.getById(id) ?: throw IllegalArgumentException("Subscription not found")
        val body = fetchBody(sub.url)
        val profiles = parseBody(body, sub.format)
        profileDao.deleteBySource(id)
        profileDao.insertAll(profiles.map { it.copy(source = id).toEntity() })
        dao.update(sub.toDomain().copy(lastUpdate = System.currentTimeMillis(), serverCount = profiles.size).toEntity())
        profiles
    }

    override suspend fun refreshAll(): Result<Unit> = runCatching {
        getAll().forEach { refresh(it.id) }
    }

    override suspend fun aggregate(subscriptionIds: List<String>): Result<Subscription> = runCatching {
        val allProfiles = subscriptionIds.flatMap { id ->
            profileDao.getAll().filter { it.source == id }.map { it.toDomain() }
        }
        Subscription(
            id = "aggregate_${UUID.randomUUID()}",
            name = "Aggregated",
            url = "",
            format = "xunnet",
            updateInterval = 0,
            enabled = true,
            tags = listOf("aggregated"),
            lastUpdate = System.currentTimeMillis(),
            serverCount = allProfiles.size,
            servers = allProfiles
        )
    }

    private suspend fun fetchBody(url: String): String {
        val response = api.fetchSubscription(url)
        if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code()}")
        return response.body()?.string() ?: throw IllegalStateException("Empty body")
    }

    private fun parseBody(body: String, format: String): List<Profile> = fetcher.parse(body)

    private fun SubscriptionEntity.toDomain(): Subscription = Subscription(
        id = id,
        name = name,
        url = url,
        format = format,
        updateInterval = updateInterval,
        enabled = enabled,
        tags = tags.split(",").filter { it.isNotBlank() },
        lastUpdate = lastUpdate,
        serverCount = serverCount
    )

    private fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
        id = id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
        name = name,
        url = url,
        format = format,
        updateInterval = updateInterval,
        enabled = enabled,
        tags = tags.joinToString(","),
        lastUpdate = lastUpdate,
        serverCount = serverCount,
        createdAt = System.currentTimeMillis()
    )

    private fun Profile.toEntity() = dev.xunnet.client.core.data.local.entity.ProfileEntity(
        id = id,
        name = name,
        protocol = protocol,
        address = address,
        port = port,
        paramsJson = paramsJson,
        tags = tags.joinToString(","),
        priority = priority,
        enabled = enabled,
        source = source,
        latencyMs = latencyMs,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    private fun dev.xunnet.client.core.data.local.entity.ProfileEntity.toDomain() = Profile(
        id = id,
        name = name,
        protocol = protocol,
        address = address,
        port = port,
        paramsJson = paramsJson,
        tags = tags.split(",").filter { it.isNotBlank() },
        priority = priority,
        enabled = enabled,
        source = source,
        latencyMs = latencyMs
    )
}
