package dev.aitalkingavatar.avatar

import kotlin.math.sin

class AvatarLipSyncController {
    var isSpeaking: Boolean = false
        private set

    private var speechStartTimeNanos: Long = 0

    fun startSpeaking(currentTimeNanos: Long) {
        isSpeaking = true
        speechStartTimeNanos = currentTimeNanos
    }

    fun stopSpeaking() {
        isSpeaking = false
    }

    fun computeMouthWeight(currentTimeNanos: Long, reducedMotion: Boolean): Float {
        if (!isSpeaking || reducedMotion) return 0f
        val elapsedSeconds = (currentTimeNanos - speechStartTimeNanos).coerceAtLeast(0) / 1_000_000_000.0
        // Smooth sine wave approximation for jaw/lip-sync movement
        return ((0.5 + 0.5 * sin(elapsedSeconds * 10.0)).toFloat() * 0.75f).coerceIn(0f, 1f)
    }
}
