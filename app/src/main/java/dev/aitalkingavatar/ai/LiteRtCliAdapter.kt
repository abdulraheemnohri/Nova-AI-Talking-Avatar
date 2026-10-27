package dev.aitalkingavatar.ai

import dev.aitalkingavatar.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LiteRtCliAdapter(
    internal val endpointUrl: String = "http://localhost:8080",
    internal val modelName: String = "litert-lm-3b"
) : AiAdapter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val activeEndpoint: String
        get() = endpointUrl.trim().trimEnd('/')

    override suspend fun reply(
        history: List<ChatMessage>,
        userText: String,
        codingMode: Boolean,
        onDelta: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = activeEndpoint.ifBlank { "http://localhost:8080" }
        val targetUrl = if (baseUrl.contains("/v1/chat/completions")) baseUrl else "$baseUrl/v1/chat/completions"

        val messagesArray = JSONArray()

        val systemPrompt = if (codingMode) {
            "You are a Google LiteRT-LM coding assistant. Provide concise explanations and well-structured code snippets."
        } else {
            "You are a friendly AI talking avatar companion powered by Google LiteRT-LM."
        }
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        for (msg in history.takeLast(10)) {
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.role == "user") "user" else "assistant")
                put("content", msg.content)
            })
        }

        val userPrompt = if (codingMode) "[Coding Mode] $userText" else userText
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        })

        val payload = JSONObject().apply {
            put("model", modelName.ifBlank { "litert-lm-3b" })
            put("messages", messagesArray)
            put("stream", true)
            put("temperature", if (codingMode) 0.2 else 0.7)
        }

        val request = Request.Builder()
            .url(targetUrl)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val fullText = StringBuilder()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "LiteRT-LM endpoint returned HTTP ${response.code}: ${response.message}"
                onDelta(err)
                return@withContext err
            }

            response.body?.charStream()?.buffered()?.use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (!trimmed.startsWith("data:")) continue
                    val jsonStr = trimmed.removePrefix("data:").trim()
                    if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue

                    runCatching {
                        val json = JSONObject(jsonStr)
                        val choices = json.optJSONArray("choices") ?: return@runCatching
                        if (choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta") ?: choice.optJSONObject("message")
                            val content = delta?.optString("content", "").orEmpty()
                            if (content.isNotEmpty()) {
                                fullText.append(content)
                                onDelta(fullText.toString())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val err = "Could not reach Google LiteRT-LM endpoint at $baseUrl: ${e.localizedMessage}"
            onDelta(err)
            return@withContext err
        }

        return@withContext if (fullText.isNotEmpty()) fullText.toString() else "No response received from LiteRT-LM endpoint."
    }

    override suspend fun testConnection(): ConnectionResult = withContext(Dispatchers.IO) {
        val baseUrl = activeEndpoint.ifBlank { "http://localhost:8080" }
        val healthUrl = "$baseUrl/v1/models"
        val request = Request.Builder().url(healthUrl).get().build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ConnectionResult(true, "Connected to Google LiteRT-LM CLI local endpoint at $baseUrl.")
            } else {
                ConnectionResult(false, "LiteRT-LM endpoint returned HTTP ${response.code}.")
            }
        } catch (e: Exception) {
            ConnectionResult(false, "Could not reach LiteRT-LM local CLI endpoint at $baseUrl: ${e.localizedMessage}")
        }
    }
}
