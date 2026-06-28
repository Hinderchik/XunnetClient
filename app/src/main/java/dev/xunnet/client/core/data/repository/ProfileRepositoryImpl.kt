package dev.xunnet.client.core.data.repository

import dev.xunnet.client.core.data.local.ProfileDao
import dev.xunnet.client.core.data.local.entity.ProfileEntity
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.parser.LinkParser
import dev.xunnet.client.core.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProfileRepositoryImpl(
    private val dao: ProfileDao,
    private val parser: LinkParser
) : ProfileRepository {

    override fun observeAll(): Flow<List<Profile>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun getAll(): List<Profile> = dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Profile? = dao.getById(id)?.toDomain()

    override suspend fun save(profile: Profile): Result<Unit> = runCatching {
        dao.insert(profile.toEntity())
    }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        dao.deleteById(id)
    }

    override suspend fun importFromLink(link: String): Result<Profile> = runCatching {
        parser.parse(link).getOrThrow().also { save(it) }
    }

    override suspend fun exportProfile(id: String): Result<String> = runCatching {
        val profile = getById(id) ?: throw IllegalArgumentException("Profile not found")
        parser.generate(profile)
    }

    private fun ProfileEntity.toDomain(): Profile = Profile(
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

    private fun Profile.toEntity(): ProfileEntity = ProfileEntity(
        id = id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
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
