package dev.aitalkingavatar.ai

import dev.aitalkingavatar.BuildConfig
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

class GeminiAdapter(
    private val apiKeyOverride: String = "",
    private val modelOverride: String = ""
) : AiAdapter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val activeModel: String
        get() = modelOverride.ifBlank { "gemini-3.5-flash" }

    private val effectiveApiKey: String
        get() {
            val key = if (apiKeyOverride.isNotBlank()) apiKeyOverride.trim() else runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("").trim()
            if (key.isBlank() || key.contains("placeholder", ignoreCase = true) || key.contains("your_key", ignoreCase = true)) return ""
            return key
        }


    override suspend fun reply(
        history: List<ChatMessage>,
        userText: String,
        codingMode: Boolean,
        onDelta: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val apiKey = effectiveApiKey
        if (apiKey.isBlank()) {
            val errorMsg = "Gemini API key is missing. Set your key in Settings or AI Studio Secrets."
            onDelta(errorMsg)
            return@withContext errorMsg
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:streamGenerateContent?alt=sse&key=$apiKey"

        val contentsArray = JSONArray()
        val relevantHistory = history.takeLast(12)
        for (msg in relevantHistory) {
            val role = if (msg.role == "user") "user" else "model"
            val contentObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
            }
            contentsArray.put(contentObj)
        }

        val promptText = if (codingMode) {
            "[Coding Mode Active] Provide a structured answer with code examples where applicable:\n$userText"
        } else userText

        val currentContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", promptText)))
        }
        contentsArray.put(currentContent)

        val systemPrompt = if (codingMode) {
            "You are an expert AI software engineering companion. Explain code clearly, provide well-formatted code snippets, and offer debugging advice."
        } else {
            "You are a friendly, intelligent AI talking avatar companion. Be concise, engaging, and clear."
        }

        val payload = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", if (codingMode) 0.2 else 0.7)
            })
        }

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        val fullText = StringBuilder()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                val parsedMsg = runCatching {
                    JSONObject(errorBody).getJSONObject("error").getString("message")
                }.getOrDefault("HTTP ${response.code}: ${response.message}")
                val err = "Gemini API call failed: $parsedMsg"
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
                        val candidates = json.optJSONArray("candidates") ?: return@runCatching
                        if (candidates.length() > 0) {
                            val cand = candidates.getJSONObject(0)
                            val content = cand.optJSONObject("content") ?: return@runCatching
                            val parts = content.optJSONArray("parts") ?: return@runCatching
                            if (parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                if (text.isNotEmpty()) {
                                    fullText.append(text)
                                    onDelta(fullText.toString())
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val errMsg = "Gemini API error: ${e.localizedMessage}"
            onDelta(errMsg)
            return@withContext errMsg
        }

        return@withContext if (fullText.isNotEmpty()) fullText.toString() else "No response generated by Gemini."
    }

    override suspend fun testConnection(): ConnectionResult = withContext(Dispatchers.IO) {
        val apiKey = effectiveApiKey
        if (apiKey.isBlank()) {
            return@withContext ConnectionResult(false, "Gemini API key is not configured.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:generateContent?key=$apiKey"
        val payload = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", "Hi")))
            }))
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ConnectionResult(true, "Connected to Google Gemini API ($activeModel).")
            } else {
                val body = response.body?.string().orEmpty()
                val msg = runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrDefault("HTTP ${response.code}")
                ConnectionResult(false, "Gemini API error: $msg")
            }
        } catch (e: Exception) {
            ConnectionResult(false, "Could not reach Google Gemini API: ${e.localizedMessage}")
        }
    }
}
