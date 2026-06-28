package dev.xunnet.client.core.data.repository

import dev.xunnet.client.core.data.local.ProfileDao
import dev.xunnet.client.core.data.local.entity.ProfileEntity
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.parser.LinkParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRepositoryImplTest {

    private val dao: ProfileDao = mockk(relaxed = true)
    private val parser: LinkParser = mockk()
    private val repository = ProfileRepositoryImpl(dao, parser)

    @Test
    fun `save profile inserts entity`() = runBlocking {
        val profile = Profile(
            id = "1",
            name = "Test",
            protocol = "vless",
            address = "example.com",
            port = 443,
            paramsJson = "{}",
            tags = emptyList(),
            priority = 5,
            enabled = true
        )
        coEvery { dao.insert(any()) } returns Unit
        val result = repository.save(profile)
        assertTrue(result.isSuccess)
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `getAll returns mapped profiles`() = runBlocking {
        coEvery { dao.getAll() } returns listOf(
            ProfileEntity(
                id = "1", name = "Test", protocol = "vless", address = "example.com",
                port = 443, paramsJson = "{}", tags = "", priority = 5, enabled = true,
                source = null, latencyMs = null, createdAt = 0, updatedAt = 0
            )
        )
        val result = repository.getAll()
        assertEquals(1, result.size)
        assertEquals("Test", result.first().name)
    }
}
