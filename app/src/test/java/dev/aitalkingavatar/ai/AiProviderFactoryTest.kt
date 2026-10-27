package dev.aitalkingavatar.ai

import dev.aitalkingavatar.data.AiProviderType
import dev.aitalkingavatar.data.AvatarPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertThrows

class AiProviderFactoryTest {
    @Test fun testOfflineDemoProviderCreation() {
        val adapter = AiProviderFactory.createAdapter(AvatarPreferences(providerType = AiProviderType.OFFLINE_DEMO))
        assertTrue(adapter is OfflineDemoAdapter)
    }

    @Test fun testOllamaProviderCreation() {
        val adapter = AiProviderFactory.createAdapter(AvatarPreferences(providerType = AiProviderType.OLLAMA, ollamaModel = "qwen2.5:3b", allowInsecureHttp = true))
        assertTrue(adapter is OllamaAdapter)
    }

    @Test fun testLiteRtProviderCreation() {
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

    @Test fun testLiteRtRejectsHttpWhenSecuritySettingDisablesIt() {
        val prefs = AvatarPreferences(
            providerType = AiProviderType.LITERT_CLI,
            liteRtEndpoint = "http://model.local:8080",
            allowInsecureHttp = false
        )
        assertThrows(IllegalArgumentException::class.java) { AiProviderFactory.createAdapter(prefs) }
    }

    @Test fun testLiteRtRejectsEndpointCredentialsAndQueryParameters() {
        assertThrows(IllegalArgumentException::class.java) {
            LiteRtCliAdapter(endpointUrl = "https://user:secret@model.local:8080")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LiteRtCliAdapter(endpointUrl = "https://model.local:8080?token=secret")
        }
    }

    @Test fun testGeminiProviderCreation() {
        val adapter = AiProviderFactory.createAdapter(AvatarPreferences(providerType = AiProviderType.GEMINI_API))
        assertTrue(adapter is GeminiAdapter)
    }
}
