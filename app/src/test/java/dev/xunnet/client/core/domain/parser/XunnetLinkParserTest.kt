package dev.xunnet.client.core.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XunnetLinkParserTest {

    private val parser = XunnetLinkParser()

    @Test
    fun `parse valid xunnet link`() {
        val link = "xunnet://chacha20-ietf-poly1305@us1.xun.net:443/?protocol=vless&uuid=123e4567-e89b-12d3-a456-426614174000&tls=true#US_VIP_01"
        val result = parser.parse(link)
        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals("US_VIP_01", profile.name)
        assertEquals("vless", profile.protocol)
        assertEquals("us1.xun.net", profile.address)
        assertEquals(443, profile.port)
    }

    @Test
    fun `generate xunnet link`() {
        val profile = dev.xunnet.client.core.domain.model.Profile(
            id = "1",
            name = "US_VIP_01",
            protocol = "vless",
            address = "us1.xun.net",
            port = 443,
            paramsJson = "{\"uuid\":\"123e4567-e89b-12d3-a456-426614174000\",\"tls\":true}",
            tags = listOf("premium", "us"),
            priority = 10,
            enabled = true
        )
        val link = parser.generate(profile)
        assertTrue(link.startsWith("xunnet://"))
        assertTrue(link.contains("us1.xun.net"))
        assertTrue(link.contains("US_VIP_01"))
    }

    @Test
    fun `parse invalid link returns failure`() {
        val result = parser.parse("not-a-link")
        assertTrue(result.isFailure)
    }
}
