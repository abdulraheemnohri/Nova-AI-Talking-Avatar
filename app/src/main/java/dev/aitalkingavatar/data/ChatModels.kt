package dev.aitalkingavatar.data

data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>
)
