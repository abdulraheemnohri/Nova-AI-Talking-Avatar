package dev.aitalkingavatar.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiAdapterTest {

    @Test
    fun testEmptyApiKeyHandling() = runBlocking {
        val adapter = GeminiAdapter(apiKeyOverride = "  ")
        val result = adapter.testConnection()
        assertFalse(result.connected)
        assertTrue(result.message.contains("key is not configured"))
    }

    @Test
    fun testReplyWithEmptyKey() = runBlocking {
        val adapter = GeminiAdapter(apiKeyOverride = "")
        var deltaReceived = ""
        val reply = adapter.reply(emptyList(), "Hello", false) { deltaReceived = it }
        assertTrue(reply.contains("key is missing"))
        assertEquals(reply, deltaReceived)
    }
}
