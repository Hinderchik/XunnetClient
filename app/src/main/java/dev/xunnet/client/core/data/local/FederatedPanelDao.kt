package dev.xunnet.client.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.xunnet.client.core.data.local.entity.FederatedPanelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FederatedPanelDao {
    @Query("SELECT * FROM federated_panels ORDER BY name ASC")
    fun observeAll(): Flow<List<FederatedPanelEntity>>

    @Query("SELECT * FROM federated_panels ORDER BY name ASC")
    suspend fun getAll(): List<FederatedPanelEntity>

    @Query("SELECT * FROM federated_panels WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FederatedPanelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(panel: FederatedPanelEntity)

    @Update
    suspend fun update(panel: FederatedPanelEntity)

    @Delete
    suspend fun delete(panel: FederatedPanelEntity)

    @Query("DELETE FROM federated_panels WHERE id = :id")
    suspend fun deleteById(id: String)
}
