package dev.aitalkingavatar.data

object SettingsRules {
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
            speechRate = input.speechRate.coerceIn(0.5f, 2f),
            pitch = input.pitch.coerceIn(0.5f, 2f),
            cameraZoom = input.cameraZoom.coerceIn(0.5f, 2f)
        )
    }
}
