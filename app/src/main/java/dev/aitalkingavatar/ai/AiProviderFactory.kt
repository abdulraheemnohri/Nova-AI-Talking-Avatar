package dev.aitalkingavatar.ai

import dev.aitalkingavatar.data.AiProviderType
import dev.aitalkingavatar.data.AvatarPreferences
import dev.aitalkingavatar.data.ChatMessage

class InactiveAdapterFallback(private val providerName: String) : AiAdapter {
    override suspend fun reply(
        history: List<ChatMessage>,
        userText: String,
        codingMode: Boolean,
        onDelta: suspend (String) -> Unit
    ): String {
        val message = "$providerName adapter is currently deactivated in Settings. Enable this adapter in Settings to send requests, or switch to Offline Demo mode."
        onDelta(message)
        return message
    }

    override suspend fun testConnection(): ConnectionResult {
        return ConnectionResult(false, "$providerName adapter is marked inactive in Settings.")
    }
}

object AiProviderFactory {
    fun createAdapter(preferences: AvatarPreferences): AiAdapter {
        if (!preferences.isCurrentProviderActive) {
            return InactiveAdapterFallback(preferences.providerType.displayName)
        }
        return when (preferences.providerType) {
            AiProviderType.OFFLINE_DEMO -> OfflineDemoAdapter()
            AiProviderType.OLLAMA -> OllamaAdapter(preferences)
            AiProviderType.LITERT_CLI -> LiteRtCliAdapter(
                endpointUrl = preferences.ollamaEndpoint.ifBlank { preferences.endpoint },
                modelName = preferences.liteRtModel
            )
            AiProviderType.GEMINI_API -> GeminiAdapter(
                apiKeyOverride = preferences.geminiApiKey,
                modelOverride = preferences.geminiModel
            )
        }
    }
}
