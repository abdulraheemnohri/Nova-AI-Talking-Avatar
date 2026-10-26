package dev.aitalkingavatar.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationClipMapperTest {
    @Test fun mapsClipsByDiscoveredNamesRatherThanAssumedIndices() {
        val selection = AnimationClipMapper.select(AvatarState.SPEAKING, listOf("Walk", "Avatar_Talk_Loop", "Idle"))
        assertEquals(1, selection.index)
        assertEquals("Avatar_Talk_Loop", selection.matchedName)
        assertFalse(selection.approximateTalkMotion)
    }

    @Test fun usesFirstDiscoveredClipAsSafeFallbackAndMarksApproximateSpeech() {
        val selection = AnimationClipMapper.select(AvatarState.SPEAKING, listOf("Breathing", "Wave"))
        assertEquals(0, selection.index)
        assertTrue(selection.approximateTalkMotion)
    }

    @Test fun supportsModelsWithoutAnyAnimationClips() {
        val selection = AnimationClipMapper.select(AvatarState.LISTENING, emptyList())
        assertNull(selection.index)
        assertNull(selection.matchedName)
    }

    @Test fun idleStateFindsRestOrBreathingClip() {
        assertEquals(1, AnimationClipMapper.select(AvatarState.IDLE, listOf("Wave", "Breathing")).index)
    }
}
