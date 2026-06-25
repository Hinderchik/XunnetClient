package dev.xunnet.client.core.domain.repository

import dev.xunnet.client.core.domain.model.FederatedPanel
import dev.xunnet.client.core.domain.model.Profile

interface FederationRepository {
    suspend fun getAllPanels(): List<FederatedPanel>
    suspend fun addPanel(panel: FederatedPanel): Result<Unit>
    suspend fun updatePanel(panel: FederatedPanel): Result<Unit>
    suspend fun deletePanel(id: String): Result<Unit>
    suspend fun syncPanel(id: String): Result<List<Profile>>
    suspend fun syncAllPanels(): Result<Unit>
    suspend fun getPanelStatus(id: String): PanelStatus
}

data class PanelStatus(
    val online: Boolean,
    val lastError: String? = null,
    val serversCount: Int = 0
)
