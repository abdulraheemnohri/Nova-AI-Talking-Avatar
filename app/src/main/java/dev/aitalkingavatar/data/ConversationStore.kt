package dev.aitalkingavatar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ConversationStore(context: Context) {
    private val prefs = context.getSharedPreferences("avatar_conversations", Context.MODE_PRIVATE)

    fun load(): List<Conversation> = runCatching {
        val root = JSONArray(prefs.getString("items", "[]") ?: "[]")
        (0 until root.length()).mapNotNull { index ->
            val item = root.optJSONObject(index) ?: return@mapNotNull null
            val messages = item.optJSONArray("messages") ?: JSONArray()
            Conversation(
                id = item.optString("id"),
                title = item.optString("title", "Conversation"),
                updatedAt = item.optLong("updatedAt"),
                messages = (0 until messages.length()).mapNotNull { mi ->
                    messages.optJSONObject(mi)?.let { msg ->
                        ChatMessage(msg.optString("id"), msg.optString("role"), msg.optString("content"), msg.optLong("createdAt"))
                    }
                }
            )
        }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    fun save(conversations: List<Conversation>) {
        val root = JSONArray()
        conversations.forEach { conversation ->
            val messages = JSONArray()
            conversation.messages.forEach { message ->
                messages.put(JSONObject().put("id", message.id).put("role", message.role)
                    .put("content", message.content).put("createdAt", message.createdAt))
            }
            root.put(JSONObject().put("id", conversation.id).put("title", conversation.title)
                .put("updatedAt", conversation.updatedAt).put("messages", messages))
        }
        prefs.edit().putString("items", root.toString()).apply()
    }

    fun clear() { prefs.edit().remove("items").apply() }

    fun exportJson(): String = JSONObject().put("exportedAt", System.currentTimeMillis())
        .put("conversations", JSONArray().also { array -> load().forEach { c ->
            val messages = JSONArray()
            c.messages.forEach { m -> messages.put(JSONObject().put("role", m.role).put("content", m.content).put("createdAt", m.createdAt)) }
            array.put(JSONObject().put("title", c.title).put("updatedAt", c.updatedAt).put("messages", messages))
        } }).toString(2)
}
