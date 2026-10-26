package dev.aitalkingavatar.data

import android.content.Context

class PreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("avatar_settings", Context.MODE_PRIVATE)

    fun load(): AvatarPreferences = AvatarPreferences(
        providerType = runCatching {
            AiProviderType.valueOf(prefs.getString("provider_type", "OFFLINE_DEMO") ?: "OFFLINE_DEMO")
        }.getOrDefault(AiProviderType.OFFLINE_DEMO),
        ollamaActive = prefs.getBoolean("ollama_active", true),
        ollamaEndpoint = prefs.getString("ollama_endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT,
        ollamaModel = prefs.getString("ollama_model", "qwen2.5:3b") ?: "qwen2.5:3b",
        liteRtActive = prefs.getBoolean("litert_active", true),
        liteRtEndpoint = prefs.getString("litert_endpoint", "http://localhost:8080") ?: "http://localhost:8080",
        liteRtModel = prefs.getString("litert_model", "litert-lm-3b") ?: "litert-lm-3b",
        geminiActive = prefs.getBoolean("gemini_active", true),
        geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
        geminiModel = prefs.getString("gemini_model", "gemini-3.5-flash") ?: "gemini-3.5-flash",
        allowInsecureHttp = prefs.getBoolean("allow_http", true),
        voiceLocale = prefs.getString("voice_locale", "en-US") ?: "en-US",
        voiceName = prefs.getString("voice_name", "") ?: "",
        speechRate = prefs.getFloat("speech_rate", 1f).coerceIn(0.5f, 2f),
        pitch = prefs.getFloat("pitch", 1f).coerceIn(0.5f, 2f),
        graphicsQuality = runCatching {
            GraphicsQuality.valueOf(prefs.getString("graphics", "BALANCED") ?: "BALANCED")
        }.getOrDefault(GraphicsQuality.BALANCED),
        cameraZoom = prefs.getFloat("camera_zoom", 1f).coerceIn(0.5f, 2f),
        reducedMotion = prefs.getBoolean("reduced_motion", false),
        codingMode = prefs.getBoolean("coding_mode", false),
        characterPath = prefs.getString("character_path", null),
        themeName = prefs.getString("theme_name", "dark_sci_fi") ?: "dark_sci_fi"
    )

    fun save(settings: AvatarPreferences) {
        val normalized = SettingsRules.normalize(settings)
        prefs.edit()
            .putString("provider_type", normalized.providerType.name)
            .putBoolean("ollama_active", normalized.ollamaActive)
            .putString("ollama_endpoint", normalized.ollamaEndpoint)
            .putString("ollama_model", normalized.ollamaModel)
            .putBoolean("litert_active", normalized.liteRtActive)
            .putString("litert_endpoint", normalized.liteRtEndpoint)
            .putString("litert_model", normalized.liteRtModel)
            .putBoolean("gemini_active", normalized.geminiActive)
            .putString("gemini_api_key", normalized.geminiApiKey)
            .putString("gemini_model", normalized.geminiModel)
            .putBoolean("allow_http", normalized.allowInsecureHttp)
            .putString("voice_locale", normalized.voiceLocale)
            .putString("voice_name", normalized.voiceName)
            .putFloat("speech_rate", normalized.speechRate)
            .putFloat("pitch", normalized.pitch)
            .putString("graphics", normalized.graphicsQuality.name)
            .putFloat("camera_zoom", normalized.cameraZoom)
            .putBoolean("reduced_motion", normalized.reducedMotion)
            .putBoolean("coding_mode", normalized.codingMode)
            .putString("character_path", normalized.characterPath)
            .putString("theme_name", normalized.themeName)
            .apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}
