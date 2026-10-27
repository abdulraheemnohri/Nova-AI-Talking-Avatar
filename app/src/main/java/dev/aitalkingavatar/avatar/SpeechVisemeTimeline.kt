package dev.aitalkingavatar.avatar

data class VisemeFrame(val timeMs: Long, val weights: Map<String, Float>)

/**
 * Samples caller-supplied viseme weights. This utility does not infer phonemes from audio;
 * callers must supply timestamped viseme data from a TTS/alignment layer.
 */
class SpeechVisemeTimeline(frames: List<VisemeFrame>) {
    val frames: List<VisemeFrame> = frames
        .map { frame ->
            VisemeFrame(
                frame.timeMs.coerceAtLeast(0L),
                frame.weights.entries.mapNotNull { (name, weight) ->
                    canonicalName(name)?.let { it to weight.coerceIn(0f, 1f) }
                }.toMap()
            )
        }
        .sortedBy { it.timeMs }
        .distinctBy { it.timeMs }

    fun sample(timeMs: Long): Map<String, Float> {
        if (frames.isEmpty()) return emptyMap()
        if (timeMs <= frames.first().timeMs) return frames.first().weights
        if (timeMs >= frames.last().timeMs) return frames.last().weights
        val rightIndex = frames.indexOfFirst { it.timeMs >= timeMs }
        if (rightIndex <= 0) return frames.first().weights
        val left = frames[rightIndex - 1]
        val right = frames[rightIndex]
        val span = (right.timeMs - left.timeMs).coerceAtLeast(1L).toFloat()
        val amount = ((timeMs - left.timeMs).toFloat() / span).coerceIn(0f, 1f)
        return (left.weights.keys + right.weights.keys).associateWith { key ->
            val start = left.weights[key] ?: 0f
            val end = right.weights[key] ?: 0f
            (start + (end - start) * amount).coerceIn(0f, 1f)
        }
    }

    companion object {
        fun canonicalName(rawName: String): String? {
            val name = rawName.lowercase().replace(Regex("[^a-z0-9]"), "")
            return when {
                name == "jawopen" || name == "mouthopen" || name == "openjaw" -> "jawOpen"
                name in setOf("a", "aa", "visemea", "visemeaa", "phonemea") -> "A"
                name in setOf("e", "ee", "visemee", "visemeee", "phonemee") -> "E"
                name in setOf("o", "oh", "visemeo", "visemeoh", "phonemeo") -> "O"
                name in setOf("u", "oo", "visemeu", "visemeoo", "phonemeu") -> "U"
                name in setOf("m", "visemem", "phonemem") -> "M"
                name in setOf("f", "v", "visemef", "visemefv", "phonemef") -> "F"
                name in setOf("l", "visemel", "phonemel") -> "L"
                name in setOf("w", "q", "visemewq", "phonemewq") -> "WQ"
                name in setOf("b", "p", "mbp", "visemembp", "phonemembp") -> "MBP"
                name.contains("smile") -> "smile"
                name.contains("blinkleft") || name == "eyeblinkl" -> "blinkLeft"
                name.contains("blinkright") || name == "eyeblinkr" -> "blinkRight"
                else -> null
            }
        }
    }
}
