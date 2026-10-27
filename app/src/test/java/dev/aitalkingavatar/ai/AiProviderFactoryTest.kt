package dev.aitalkingavatar.ai

import dev.aitalkingavatar.data.AiProviderType
import dev.aitalkingavatar.data.AvatarPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderFactoryTest {

    @Test
    fun testOfflineDemoProviderCreation() {
        val prefs = AvatarPreferences(providerType = AiProviderType.OFFLINE_DEMO)
        val adapter = AiProviderFactory.createAdapter(prefs)
        assertTrue(adapter is OfflineDemoAdapter)
    }

    @Test
    fun testOllamaProviderCreation() {
        val prefs = AvatarPreferences(providerType = AiProviderType.OLLAMA, ollamaModel = "qwen2.5:3b", allowInsecureHttp = true)
        val adapter = AiProviderFactory.createAdapter(prefs)
        assertTrue(adapter is OllamaAdapter)
    }




    @Test
    fun testLiteRtProviderCreation() {
        val prefs = AvatarPreferences(
            providerType = AiProviderType.LITERT_CLI,
            ollamaEndpoint = "http://ollama-host:11434",
            liteRtEndpoint = "http://litert-host:8080",
            liteRtModel = "gemma-4-e2b"
        )
        val adapter = AiProviderFactory.createAdapter(prefs)
        assertTrue(adapter is LiteRtCliAdapter)
        assertEquals("http://litert-host:8080", (adapter as LiteRtCliAdapter).endpointUrl)
        assertEquals("gemma-4-e2b", adapter.modelName)
    }

    @Test
    fun testGeminiProviderCreation() {
        val prefs = AvatarPreferences(providerType = AiProviderType.GEMINI_API)
        val adapter = AiProviderFactory.createAdapter(prefs)
        assertTrue(adapter is GeminiAdapter)
    }
}
