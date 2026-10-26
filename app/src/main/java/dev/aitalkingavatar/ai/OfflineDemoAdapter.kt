package dev.aitalkingavatar.ai

import dev.aitalkingavatar.data.ChatMessage
import kotlinx.coroutines.delay

class OfflineDemoAdapter : AiAdapter {
    override suspend fun reply(history: List<ChatMessage>, userText: String, codingMode: Boolean, onDelta: suspend (String) -> Unit): String {
        val text = userText.trim()
        val answer = when {
            codingMode && text.contains("hello", ignoreCase = true) -> "Here is a small Kotlin example:\n\n```kotlin\nfun greet(name: String): String = \"Hello, \${'$'}name!\"\n```\n\nThis is the offline demo response. Select an Ollama-compatible model in Settings for model-generated coding help."
            codingMode -> "I’m in Coding Help mode, but no local model is configured, so I can’t inspect or execute project code.\n\nTry connecting a locally running Ollama-compatible endpoint in Settings. This offline demo can still show formatting and interaction behavior; answer quality depends on the selected local model."
            text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) -> "Hello. I’m running in offline demo mode, so your messages stay on this device. Add a local model endpoint in Settings when you want model-generated answers."
            text.contains("offline", ignoreCase = true) -> "Offline demo mode works without an AI model. Chat history stays on this device. Speech recognition and text-to-speech depend on the Android provider and installed language data."
            text.contains("help", ignoreCase = true) -> "I can accept typed messages and one-shot microphone input, save local conversations, and speak replies. For generated answers, configure a local Ollama-compatible endpoint and a model name in Settings."
            else -> "I received: “${text.take(300)}”. This is the rule-based offline demo, not an AI-generated answer. Configure a local model in Settings to enable richer responses."
        }
        val chunks = answer.chunked(72)
        val assembled = StringBuilder()
        for (chunk in chunks) {
            delay(12)
            assembled.append(chunk)
            onDelta(assembled.toString())
        }
        return answer
    }

    override suspend fun testConnection() = ConnectionResult(false, "Offline demo is active. Add a model endpoint and model name to test a local connection.")
}
