package dev.xunnet.client.core.domain.repository

import dev.xunnet.client.core.domain.model.Profile
import java.io.File

interface ProfileRepository {
    suspend fun getAll(): List<Profile>
    suspend fun getById(id: String): Profile?
    suspend fun save(profile: Profile): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun importFromLink(link: String): Result<Profile>
    suspend fun exportProfile(id: String): Result<String>
    suspend fun importFromFile(file: File): Result<List<Profile>>
}
