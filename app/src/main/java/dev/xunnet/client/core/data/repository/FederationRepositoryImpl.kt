package dev.xunnet.client.core.data.repository

import dev.xunnet.client.core.data.local.FederatedPanelDao
import dev.xunnet.client.core.data.local.ProfileDao
import dev.xunnet.client.core.data.local.entity.FederatedPanelEntity
import dev.xunnet.client.core.data.remote.FederationApi
import dev.xunnet.client.core.domain.model.FederatedPanel
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.repository.FederationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FederationRepositoryImpl(
    private val dao: FederatedPanelDao,
    private val profileDao: ProfileDao,
    private val api: FederationApi
) : FederationRepository {

    override fun observeAll(): Flow<List<FederatedPanel>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun getAllPanels(): List<FederatedPanel> = dao.getAll().map { it.toDomain() }

    override suspend fun getPanelById(id: String): FederatedPanel? = dao.getById(id)?.toDomain()

    override suspend fun addPanel(panel: FederatedPanel): Result<Unit> = runCatching {
        dao.insert(panel.toEntity())
    }

    override suspend fun updatePanel(panel: FederatedPanel): Result<Unit> = runCatching {
        dao.update(panel.toEntity())
    }

    override suspend fun deletePanel(id: String): Result<Unit> = runCatching {
        dao.deleteById(id)
        profileDao.deleteBySource(id)
    }

    override suspend fun syncPanel(id: String): Result<List<Profile>> = runCatching {
        val panel = dao.getById(id) ?: throw IllegalArgumentException("Panel not found")
        val response = api.getServers(panel.host, panel.apiKey, limit = 100)
        if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code()}")
        val servers = response.body() ?: emptyList()
        profileDao.deleteBySource(id)
        profileDao.insertAll(servers.map { it.copy(source = id).toEntity() })
        dao.update(panel.copy(lastSync = System.currentTimeMillis(), serversCount = servers.size).toEntity())
        servers
    }

    override suspend fun syncAllPanels(): Result<Unit> = runCatching {
        getAllPanels().filter { it.enabled }.forEach { syncPanel(it.id) }
    }

    override suspend fun getPanelStatus(id: String): PanelStatus {
        val entity = dao.getById(id)
        return PanelStatus(
            online = entity?.status == "online",
            serversCount = entity?.serversCount ?: 0
        )
    }

    private fun FederatedPanelEntity.toDomain(): FederatedPanel = FederatedPanel(
        id = id,
        name = name,
        host = host,
        apiKey = apiKey,
        role = role,
        mode = mode,
        status = status,
        lastSync = lastSync,
        serversCount = serversCount,
        tags = tags.split(",").filter { it.isNotBlank() },
        enabled = enabled
    )

    private fun FederatedPanel.toEntity(): FederatedPanelEntity = FederatedPanelEntity(
        id = id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
        name = name,
        host = host,
        apiKey = apiKey,
        role = role,
        mode = mode,
        status = status,
        lastSync = lastSync,
        serversCount = serversCount,
        tags = tags.joinToString(","),
        enabled = enabled,
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
}
