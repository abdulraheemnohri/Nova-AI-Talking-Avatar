package dev.aitalkingavatar.ai

import dev.aitalkingavatar.data.ChatMessage
import dev.aitalkingavatar.data.EndpointRules
import dev.aitalkingavatar.data.EndpointValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LiteRtCliAdapter(
    internal val endpointUrl: String = "http://localhost:8080",
    internal val modelName: String = "litert-lm-3b",
    allowInsecureHttp: Boolean = true
) : AiAdapter {

    private val safeEndpoint: String = when (val result = EndpointRules.validate(endpointUrl, allowInsecureHttp)) {
        is EndpointValidation.Valid -> result.normalized
        is EndpointValidation.Invalid -> throw IllegalArgumentException(result.reason)
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .build()

    private val activeEndpoint: String get() = safeEndpoint

    override suspend fun reply(
        history: List<ChatMessage>,
        userText: String,
        codingMode: Boolean,
        onDelta: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = activeEndpoint
        val targetUrl = if (baseUrl.endsWith("/v1/chat/completions")) baseUrl else "$baseUrl/v1/chat/completions"
        val messagesArray = JSONArray()
        val systemPrompt = if (codingMode) {
            "You are a Google LiteRT-LM coding assistant. Provide concise explanations and well-structured code snippets."
        } else {
            "You are a friendly AI talking avatar companion powered by Google LiteRT-LM."
        }
        messagesArray.put(JSONObject().put("role", "system").put("content", systemPrompt))
        history.takeLast(10).filter { it.role == "user" || it.role == "assistant" }.forEach { msg ->
            messagesArray.put(JSONObject().put("role", msg.role).put("content", msg.content))
        }
        messagesArray.put(JSONObject().put("role", "user").put("content", if (codingMode) "[Coding Mode] $userText" else userText))
        val payload = JSONObject().apply {
            put("model", modelName.trim().ifBlank { "litert-lm-3b" })
            put("messages", messagesArray)
            put("stream", true)
            put("temperature", if (codingMode) 0.2 else 0.7)
        }
        val request = Request.Builder().url(targetUrl)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val fullText = StringBuilder()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("LiteRT-LM endpoint returned HTTP ${response.code}.")
                val body = response.body ?: throw IOException("LiteRT-LM endpoint returned an empty response.")
                body.charStream().buffered().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        val trimmed = line.trim()
                        if (!trimmed.startsWith("data:")) continue
                        val jsonStr = trimmed.removePrefix("data:").trim()
                        if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue
                        val content = runCatching {
                            val choices = JSONObject(jsonStr).optJSONArray("choices") ?: return@runCatching ""
                            if (choices.length() == 0) "" else {
                                val choice = choices.optJSONObject(0) ?: return@runCatching ""
                                val delta = choice.optJSONObject("delta") ?: choice.optJSONObject("message")
                                delta?.optString("content", "").orEmpty()
                            }
                        }.getOrDefault("")
                        if (content.isNotEmpty()) {
                            fullText.append(content)
                            onDelta(fullText.toString())
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw IOException("Could not reach LiteRT-LM endpoint at ${EndpointRules.redacted(baseUrl)}. Check the URL, network, and server logs.")
        }
        if (fullText.isNotEmpty()) fullText.toString() else throw IOException("No response received from LiteRT-LM endpoint.")
    }

    override suspend fun testConnection(): ConnectionResult = withContext(Dispatchers.IO) {
        val healthUrl = "$activeEndpoint/v1/models"
        val request = Request.Builder().url(healthUrl).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) ConnectionResult(true, "Connected to Google LiteRT-LM CLI local endpoint at ${EndpointRules.redacted(activeEndpoint)}.")
                else ConnectionResult(false, "LiteRT-LM endpoint returned HTTP ${response.code}.")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ConnectionResult(false, "Could not reach LiteRT-LM endpoint at ${EndpointRules.redacted(activeEndpoint)}. Check that the server is running.")
        }
    }
}
