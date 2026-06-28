package dev.xunnet.client.core.data.repository

import dev.xunnet.client.core.data.local.FederatedPanelDao
import dev.xunnet.client.core.data.local.ProfileDao
import dev.xunnet.client.core.data.local.entity.FederatedPanelEntity
import dev.xunnet.client.core.data.remote.FederationApi
import dev.xunnet.client.core.domain.model.FederatedPanel
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.repository.FederationRepository
import dev.xunnet.client.core.domain.repository.PanelStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.net.URI
import java.util.UUID

class FederationRepositoryImpl(
    private val dao: FederatedPanelDao,
    private val profileDao: ProfileDao,
    private val api: FederationApi,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000
) : FederationRepository {

    override fun observeAll(): Flow<List<FederatedPanel>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun getAllPanels(): List<FederatedPanel> = dao.getAll().map { it.toDomain() }

    override suspend fun getPanelById(id: String): FederatedPanel? = dao.getById(id)?.toDomain()

    override suspend fun addPanel(panel: FederatedPanel): Result<Unit> = runCatching {
        validatePanel(panel)
        dao.insert(panel.toEntity())
    }

    override suspend fun updatePanel(panel: FederatedPanel): Result<Unit> = runCatching {
        validatePanel(panel)
        dao.update(panel.toEntity())
    }

    override suspend fun deletePanel(id: String): Result<Unit> = runCatching {
        dao.deleteById(id)
        profileDao.deleteBySource(id)
    }

    override suspend fun syncPanel(id: String): Result<List<Profile>> = runCatching {
        val panel = dao.getById(id) ?: throw IllegalArgumentException("Panel not found")
        validatePanel(panel.toDomain())

        val url = buildUrl(panel.host, "/api/v1/federation/servers")
        val response = withRetry {
            api.getServers(url, panel.apiKey, limit = 200)
        }

        if (!response.isSuccessful) {
            markPanelStatus(panel.id, "error")
            throw IllegalStateException("HTTP ${response.code()} from ${panel.host}")
        }

        val servers = response.body() ?: emptyList()
        profileDao.deleteBySource(id)
        profileDao.insertAll(servers.map { it.copy(source = id).toEntity() })
        markPanelSync(panel.id, servers.size)
        servers
    }

    override suspend fun syncAllPanels(): Result<Unit> = runCatching {
        val panels = getAllPanels().filter { it.enabled }
        var failures = 0
        for (panel in panels) {
            val r = syncPanel(panel.id)
            if (r.isFailure) {
                failures++
                Timber.w(r.exceptionOrNull(), "Failed to sync panel ${panel.name}")
            }
        }
        if (failures == panels.size && panels.isNotEmpty()) {
            throw IllegalStateException("All ${panels.size} panels failed to sync")
        }
    }

    override suspend fun getPanelStatus(id: String): PanelStatus {
        val entity = dao.getById(id)
            ?: return PanelStatus(online = false, lastError = "Panel not found")
        // If status is stale (>5 min), probe live
        val stale = entity.lastSync == null ||
            (System.currentTimeMillis() - (entity.lastSync ?: 0)) > 5 * 60 * 1000L
        return if (stale) {
            probeStatus(entity)
        } else {
            PanelStatus(
                online = entity.status == "online",
                lastError = entity.status.takeIf { it == "error" },
                serversCount = entity.serversCount
            )
        }
    }

    private suspend fun probeStatus(entity: FederatedPanelEntity): PanelStatus {
        return try {
            val infoUrl = buildUrl(entity.host, "/api/v1/federation/info")
            val response = withRetry { api.getInfo(infoUrl, entity.apiKey) }
            if (response.isSuccessful) {
                PanelStatus(online = true, serversCount = entity.serversCount)
            } else {
                PanelStatus(
                    online = false,
                    lastError = "HTTP ${response.code()}",
                    serversCount = entity.serversCount
                )
            }
        } catch (e: Exception) {
            PanelStatus(online = false, lastError = e.message, serversCount = entity.serversCount)
        }
    }

    private suspend fun markPanelSync(id: String, count: Int) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(
            lastSync = System.currentTimeMillis(),
            serversCount = count,
            status = "online"
        ))
    }

    private suspend fun markPanelStatus(id: String, status: String) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(status = status))
    }

    private fun buildUrl(host: String, path: String): String {
        val normalized = if (host.startsWith("http://") || host.startsWith("https://")) host
        else "https://$host"
        val base = normalized.trimEnd('/')
        val p = if (path.startsWith("/")) path else "/$path"
        return base + p
    }

    private fun validatePanel(panel: FederatedPanel) {
        if (panel.host.isBlank()) throw IllegalArgumentException("Panel host is blank")
        if (panel.name.isBlank()) throw IllegalArgumentException("Panel name is blank")
        runCatching { URI(createPanelBaseUri(panel.host)) }.onFailure {
            throw IllegalArgumentException("Panel host is not a valid URL: ${panel.host}")
        }
    }

    private fun createPanelBaseUri(host: String): String {
        val normalized = if (host.startsWith("http://") || host.startsWith("https://")) host
        else "https://$host"
        return normalized.trimEnd('/')
    }

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                Timber.w(e, "Retry ${attempt + 1}/$maxRetries failed")
                if (attempt < maxRetries - 1) delay(retryDelayMs * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Retry exhausted")
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
