package dev.aitalkingavatar.ai

import dev.aitalkingavatar.data.AvatarPreferences
import dev.aitalkingavatar.data.ChatMessage
import dev.aitalkingavatar.data.EndpointRules
import dev.aitalkingavatar.data.EndpointValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

class OllamaAdapter(private val settings: AvatarPreferences) : AiAdapter {
    private val endpoint: String = when (val validation = EndpointRules.validate(settings.endpoint, settings.allowInsecureHttp)) {
        is EndpointValidation.Valid -> validation.normalized
        is EndpointValidation.Invalid -> throw IllegalArgumentException(validation.reason)
    }
    private val model = settings.modelName.trim().also { require(it.isNotEmpty()) { "Enter a model name in Settings." } }
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(): ConnectionResult = runCatching {
        val request = Request.Builder().url("$endpoint/api/tags").get().build()
        withResponse(request) { response ->
            if (!response.isSuccessful) return@withResponse ConnectionResult(false, "Endpoint responded with HTTP ${response.code}.")
            val models = JSONObject(response.body?.string().orEmpty()).optJSONArray("models") ?: JSONArray()
            val found = (0 until models.length()).any { index -> models.optJSONObject(index)?.optString("name") == model }
            if (found) ConnectionResult(true, "Connected. Model is available.")
            else ConnectionResult(false, "Connected, but the selected model was not found. Check the model name or load it on the local server.")
        }
    }.getOrElse {
        if (it is CancellationException) throw it
        ConnectionResult(false, safeMessage(it))
    }

    override suspend fun reply(history: List<ChatMessage>, userText: String, codingMode: Boolean, onDelta: suspend (String) -> Unit): String {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", if (codingMode)
            "You are a coding assistant. Explain assumptions, preserve code in fenced blocks, and do not claim you ran code unless you did."
        else "You are a helpful personal talking avatar. Be clear, concise, and honest about uncertainty."))
        history.takeLast(20).forEach { item ->
            if (item.role == "user" || item.role == "assistant") messages.put(JSONObject().put("role", item.role).put("content", item.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userText))
        fun chatRequest(stream: Boolean) = Request.Builder().url("$endpoint/api/chat")
            .post(JSONObject().put("model", model).put("messages", messages).put("stream", stream).toString().toRequestBody(jsonType))
            .build()

        return try {
            val streamed = withResponse(chatRequest(stream = true)) { response ->
                if (!response.isSuccessful && response.code in setOf(400, 404, 405, 422)) null
                else parseChatResponse(response, streamed = true, onDelta)
            }
            streamed ?: withResponse(chatRequest(stream = false)) { response -> parseChatResponse(response, streamed = false, onDelta) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (error is IllegalArgumentException && error.message == "Enter a model name in Settings.") throw error
            throw IOException("Local model request failed. Check the endpoint, model name, and Ollama-compatible response format.")
        }
    }

    private suspend fun parseChatResponse(response: Response, streamed: Boolean, onDelta: suspend (String) -> Unit): String {
        if (!response.isSuccessful) throw IOException("Local model returned HTTP ${response.code}.")
        val body = response.body ?: throw IOException("Local model returned an empty response.")
        if (!streamed) {
            val content = JSONObject(body.string()).optJSONObject("message")?.optString("content").orEmpty()
            if (content.isBlank()) throw IOException("The local endpoint returned an empty chat response.")
            withContext(Dispatchers.Main.immediate) { onDelta(content) }
            return content
        }
        val reader = BufferedReader(body.charStream())
        val accumulated = StringBuilder()
        while (true) {
            currentCoroutineContext().ensureActive()
            val line = reader.readLine() ?: break
            val raw = line.removePrefix("data:").trim()
            if (raw.isBlank() || raw == "[DONE]") continue
            val delta = runCatching { JSONObject(raw).optJSONObject("message")?.optString("content").orEmpty() }.getOrDefault("")
            if (delta.isNotEmpty()) {
                accumulated.append(delta)
                val snapshot = accumulated.toString()
                withContext(Dispatchers.Main.immediate) { onDelta(snapshot) }
            }
        }
        if (accumulated.isEmpty()) throw IOException("No text was returned. Confirm that the server supports the Ollama /api/chat format.")
        return accumulated.toString()
    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun <T> withResponse(request: Request, block: suspend (Response) -> T): T = withContext(Dispatchers.IO) {
        val call: Call = client.newCall(request)
        val cancellationHandle = currentCoroutineContext()[Job]?.invokeOnCompletion(onCancelling = true, invokeImmediately = true) { cause ->
            if (cause is CancellationException) call.cancel()
        }
        try {
            call.execute().use { response -> block(response) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw IOException("Local endpoint request failed. Check the URL, network, and timeout.")
        } finally {
            cancellationHandle?.dispose()
        }
    }

    private fun safeMessage(error: Throwable): String = when (error) {
        is IllegalArgumentException -> error.message ?: "Settings are incomplete."
        else -> "Could not reach ${EndpointRules.redacted(settings.endpoint)}. Check that the local server is running and reachable."
    }
}
