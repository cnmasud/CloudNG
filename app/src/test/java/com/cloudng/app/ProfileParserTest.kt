package com.cloudng.app

import com.cloudng.app.core.ParseResult
import com.cloudng.app.core.ProfileParser
import com.cloudng.app.data.model.Protocol
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProfileParserTest {

    private lateinit var parser: ProfileParser

    @Before
    fun setUp() {
        parser = ProfileParser()
    }

    @Test
    fun `parse vless URI returns success with correct fields`() {
        val uri = "vless://abc-uuid-123@example.com:443?type=tcp&security=tls&sni=example.com#MyServer"
        val result = parser.parse(uri)
        assertTrue(result is ParseResult.Success)
        val profile = (result as ParseResult.Success).profiles.first()
        assertEquals(Protocol.VLESS, profile.protocol)
        assertEquals("example.com", profile.address)
        assertEquals(443, profile.port)
        assertEquals("abc-uuid-123", profile.uuid)
        assertEquals("MyServer", profile.remarks)
    }

    @Test
    fun `parse trojan URI returns success with password`() {
        val uri = "trojan://mypassword@trojan.example.com:443?sni=trojan.example.com#TrojanServer"
        val result = parser.parse(uri)
        assertTrue(result is ParseResult.Success)
        val profile = (result as ParseResult.Success).profiles.first()
        assertEquals(Protocol.TROJAN, profile.protocol)
        assertEquals("mypassword", profile.password)
        assertEquals(443, profile.port)
    }

    @Test
    fun `parse invalid input returns failure`() {
        val result = parser.parse("not_a_valid_uri")
        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun `parse empty string returns failure`() {
        val result = parser.parse("")
        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun `parse vmess base64 returns success`() {
        val vmessJson = """{"v":"2","ps":"TestNode","add":"vmess.example.com","port":"8443","id":"test-uuid","aid":"0","net":"ws","type":"none","host":"vmess.example.com","path":"/ws","tls":"tls","sni":""}"""
        val encoded = android.util.Base64.encodeToString(
            vmessJson.toByteArray(), android.util.Base64.NO_WRAP
        )
        val uri = "vmess://$encoded"
        val result = parser.parse(uri)
        assertTrue("Expected Success but got: $result", result is ParseResult.Success)
        val profile = (result as ParseResult.Success).profiles.first()
        assertEquals(Protocol.VMESS, profile.protocol)
        assertEquals("vmess.example.com", profile.address)
    }
}
