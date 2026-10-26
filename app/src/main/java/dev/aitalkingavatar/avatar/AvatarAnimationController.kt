package dev.aitalkingavatar.avatar

class AvatarAnimationController {
    data class AnimationClip(val name: String, val durationSeconds: Float, val loop: Boolean)

    private val availableClips = mutableMapOf<String, AnimationClip>()

    fun registerClip(name: String, durationSeconds: Float, loop: Boolean) {
        availableClips[name] = AnimationClip(name, durationSeconds, loop)
    }

    fun selectClipForState(state: AvatarState, clipNames: List<String>): String? {
        val keywords = when (state) {
            AvatarState.IDLE -> listOf("idle", "rest", "stand", "breath")
            AvatarState.LISTENING -> listOf("listen", "hear", "attention")
            AvatarState.THINKING -> listOf("think", "ponder", "wait")
            AvatarState.SPEAKING -> listOf("speak", "talk", "chat", "phoneme", "jaw")
            AvatarState.ERROR -> listOf("error", "sad", "concern")
            AvatarState.PAUSED -> listOf("idle", "rest")
        }
        return clipNames.firstOrNull { name -> keywords.any { key -> name.contains(key, ignoreCase = true) } }
            ?: clipNames.firstOrNull()
    }
}
