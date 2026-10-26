package dev.aitalkingavatar.data

data class DiagnosticSummary(
    val adapter: String,
    val endpoint: String,
    val model: String,
    val voiceStatus: String,
    val characterStatus: String,
    val lastError: String,
    val events: List<String>
)

fun diagnosticSummary(settings: AvatarPreferences, hasCharacter: Boolean, lastError: String?, events: List<String> = emptyList()): DiagnosticSummary = DiagnosticSummary(
    adapter = if (settings.modelName.isBlank()) "Offline demo" else "Local Ollama-compatible endpoint",
    endpoint = EndpointRules.redacted(settings.endpoint),
    model = settings.modelName.ifBlank { "Not configured" },
    voiceStatus = "Android speech services; availability depends on installed provider/language",
    characterStatus = if (hasCharacter) "Local GLB selected" else "No character asset imported",
    lastError = lastError?.replace(Regex("(?i)(token|key|secret)=\\S+")) { "${it.groupValues[1]}=[redacted]" }?.take(240) ?: "None",
    events = events.take(20).map { it.replace(Regex("(?i)(token|key|secret)=\\S+")) { match -> "${match.groupValues[1]}=[redacted]" }.take(200) }
)
