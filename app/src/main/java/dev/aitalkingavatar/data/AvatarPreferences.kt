package dev.aitalkingavatar.data

import java.net.URI

const val DEFAULT_ENDPOINT = "http://localhost:11434"

enum class AiProviderType(val displayName: String) {
    OFFLINE_DEMO("Offline Demo (Rules-based)"),
    OLLAMA("Ollama Local Endpoint"),
    LITERT_CLI("Google LiteRT-LM CLI"),
    GEMINI_API("Google Gemini API")
}

data class AvatarPreferences(
    val providerType: AiProviderType = AiProviderType.OFFLINE_DEMO,
    val ollamaActive: Boolean = true,
    val ollamaEndpoint: String = DEFAULT_ENDPOINT,
    val ollamaModel: String = "qwen2.5:3b",
    val liteRtActive: Boolean = true,
    val liteRtEndpoint: String = "http://localhost:8080",
    val liteRtModel: String = "litert-lm-3b",
    val geminiActive: Boolean = true,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.5-flash",
    val allowInsecureHttp: Boolean = true,
    val voiceLocale: String = "en-US",
    val voiceName: String = "",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.BALANCED,
    val cameraZoom: Float = 1.0f,
    val reducedMotion: Boolean = false,
    val codingMode: Boolean = false,
    val characterPath: String? = null,
    val themeName: String = "dark_sci_fi"
) {
    val endpoint: String
        get() = when (providerType) {
            AiProviderType.OLLAMA -> ollamaEndpoint
            AiProviderType.LITERT_CLI -> liteRtEndpoint
            else -> ollamaEndpoint
        }

    val modelName: String
        get() = when (providerType) {
            AiProviderType.OLLAMA -> ollamaModel
            AiProviderType.LITERT_CLI -> liteRtModel
            AiProviderType.GEMINI_API -> geminiModel
            else -> ""
        }

    val isCurrentProviderActive: Boolean
        get() = when (providerType) {
            AiProviderType.OFFLINE_DEMO -> true
            AiProviderType.OLLAMA -> ollamaActive
            AiProviderType.LITERT_CLI -> liteRtActive
            AiProviderType.GEMINI_API -> geminiActive
        }
}

enum class GraphicsQuality { LOW, BALANCED, HIGH }

sealed interface EndpointValidation {
    data class Valid(val normalized: String) : EndpointValidation
    data class Invalid(val reason: String) : EndpointValidation
}

object EndpointRules {
    fun validate(value: String, allowInsecureHttp: Boolean): EndpointValidation {
        val raw = value.trim().trimEnd('/')
        if (raw.isEmpty()) return EndpointValidation.Invalid("Enter a local model endpoint.")
        val uri = try { URI(raw) } catch (_: Exception) {
            return EndpointValidation.Invalid("Endpoint must be a valid URL.")
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme !in setOf("http", "https")) return EndpointValidation.Invalid("Use HTTPS or HTTP local endpoint.")
        if (uri.host.isNullOrBlank() || uri.userInfo != null) return EndpointValidation.Invalid("Endpoint host is required; credentials in URLs are not supported.")
        if (uri.fragment != null) return EndpointValidation.Invalid("Endpoint URLs cannot contain fragments.")
        if (uri.query != null) return EndpointValidation.Invalid("Endpoint query parameters are not supported.")
        if (scheme == "http" && !allowInsecureHttp) return EndpointValidation.Invalid("HTTP is disabled in security rules. Toggle 'Allow Insecure HTTP' to enable.")
        val port = uri.port
        if (port !in -1..65535) return EndpointValidation.Invalid("Endpoint port is invalid.")
        return EndpointValidation.Valid(raw)
    }

    fun redacted(value: String): String = try {
        val uri = URI(value)
        URI(uri.scheme, null, uri.host, uri.port, uri.path, null, null).toString().trimEnd('/')
    } catch (_: Exception) { "(not configured)" }
}
