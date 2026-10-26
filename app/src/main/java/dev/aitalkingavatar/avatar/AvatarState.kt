package dev.aitalkingavatar.avatar

enum class AvatarState(val label: String) {
    IDLE("Ready"), LISTENING("Listening"), THINKING("Thinking"), SPEAKING("Speaking"), ERROR("Needs attention"), PAUSED("Paused")
}


data class ClipSelection(val index: Int?, val matchedName: String?, val approximateTalkMotion: Boolean)

object AnimationClipMapper {
    fun select(state: AvatarState, clipNames: List<String>): ClipSelection {
        val names = clipNames.mapIndexed { index, name -> index to name }
        val keywords = when (state) {
            AvatarState.IDLE -> listOf("idle", "rest", "stand", "breath")
            AvatarState.LISTENING -> listOf("listen", "hear", "attention")
            AvatarState.THINKING -> listOf("think", "ponder", "wait")
            AvatarState.SPEAKING -> listOf("speak", "talk", "chat", "phoneme", "jaw")
            AvatarState.ERROR -> listOf("error", "sad", "concern")
            AvatarState.PAUSED -> listOf("idle", "rest")
        }

        val match = names.firstOrNull { (_, name) -> keywords.any { key -> name.contains(key, ignoreCase = true) } }
        val safeFallback = names.firstOrNull { (_, name) -> name.isNotBlank() } ?: names.firstOrNull()
        val selected = match ?: safeFallback
        return ClipSelection(
            index = selected?.first,
            matchedName = selected?.second?.takeIf { it.isNotBlank() },
            approximateTalkMotion = state == AvatarState.SPEAKING && (match == null || selected?.second?.contains("talk", true) != true && selected?.second?.contains("speak", true) != true)
        )
    }
}
