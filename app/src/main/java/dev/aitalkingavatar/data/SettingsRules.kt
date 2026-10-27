package dev.aitalkingavatar.data

object SettingsRules {
    private fun boundedFinite(value: Float, fallback: Float, min: Float, max: Float): Float =
        (if (value.isFinite()) value else fallback).coerceIn(min, max)

    fun normalize(input: AvatarPreferences): AvatarPreferences {
        val trimmedOllama = input.ollamaEndpoint.trim()
        val safeOllama = when (EndpointRules.validate(trimmedOllama, allowInsecureHttp = true)) {
            is EndpointValidation.Valid -> trimmedOllama
            is EndpointValidation.Invalid -> DEFAULT_ENDPOINT
        }
        val trimmedLiteRt = input.liteRtEndpoint.trim()
        val safeLiteRt = when (EndpointRules.validate(trimmedLiteRt, allowInsecureHttp = true)) {
            is EndpointValidation.Valid -> trimmedLiteRt
            is EndpointValidation.Invalid -> "http://localhost:8080"
        }
        return input.copy(
            ollamaEndpoint = safeOllama,
            ollamaModel = input.ollamaModel.trim().ifBlank { "qwen2.5:3b" },
            liteRtEndpoint = safeLiteRt,
            liteRtModel = input.liteRtModel.trim().ifBlank { "litert-lm-3b" },
            geminiApiKey = input.geminiApiKey.trim(),
            geminiModel = input.geminiModel.trim().ifBlank { "gemini-3.5-flash" },
            voiceLocale = input.voiceLocale.trim().ifBlank { "en-US" },
            voiceName = input.voiceName.trim(),
            speechRate = boundedFinite(input.speechRate, 1f, 0.5f, 2f),
            pitch = boundedFinite(input.pitch, 1f, 0.5f, 2f),
            cameraZoom = boundedFinite(input.cameraZoom, 1f, 0.5f, 2f)
        )
    }
}
