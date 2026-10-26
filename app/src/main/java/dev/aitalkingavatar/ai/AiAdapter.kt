package dev.aitalkingavatar.ai

import dev.aitalkingavatar.data.ChatMessage

interface AiAdapter {
    suspend fun reply(
        history: List<ChatMessage>,
        userText: String,
        codingMode: Boolean,
        onDelta: suspend (String) -> Unit
    ): String

    suspend fun testConnection(): ConnectionResult
}

data class ConnectionResult(val connected: Boolean, val message: String)
