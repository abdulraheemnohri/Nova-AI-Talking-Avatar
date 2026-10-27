package dev.aitalkingavatar.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechVisemeTimelineTest {
    @Test fun interpolatesBetweenTimestampedFrames() {
        val timeline = SpeechVisemeTimeline(listOf(
            VisemeFrame(0, mapOf("jawOpen" to 0f, "viseme_A" to 0f)),
            VisemeFrame(100, mapOf("jawOpen" to 1f, "viseme_A" to 0.8f))
        ))
        val sampled = timeline.sample(50)
        assertEquals(0.5f, sampled["jawOpen"]!!, 0.0001f)
        assertEquals(0.4f, sampled["A"]!!, 0.0001f)
    }

    @Test fun clampsTimeAndWeights() {
        val timeline = SpeechVisemeTimeline(listOf(
            VisemeFrame(10, mapOf("viseme_O" to 2f)),
            VisemeFrame(20, mapOf("viseme_O" to -1f))
        ))
        assertEquals(1f, timeline.sample(0)["O"]!!, 0f)
        assertEquals(0f, timeline.sample(20)["O"]!!, 0f)
    }

    @Test fun sortsFramesAndKeepsLastDuplicateTimestamp() {
        val timeline = SpeechVisemeTimeline(listOf(
            VisemeFrame(20, mapOf("A" to 1f)),
            VisemeFrame(0, mapOf("A" to 0f)),
            VisemeFrame(20, mapOf("A" to 0.5f))
        ))
        assertEquals(2, timeline.frames.size)
        assertEquals(0.5f, timeline.sample(20)["A"]!!, 0f)
    }

    @Test fun ignoresUnknownMorphNames() {
        val timeline = SpeechVisemeTimeline(listOf(VisemeFrame(0, mapOf("customExpression" to 1f))))
        assertEquals(emptyMap<String, Float>(), timeline.sample(0))
        assertNull(SpeechVisemeTimeline.canonicalName("customExpression"))
    }

    @Test fun recognizesCommonMorphAliases() {
        assertEquals("jawOpen", SpeechVisemeTimeline.canonicalName("jaw_open"))
        assertEquals("MBP", SpeechVisemeTimeline.canonicalName("viseme_MBP"))
        assertEquals("blinkLeft", SpeechVisemeTimeline.canonicalName("EyeBlinkLeft"))
    }

    @Test fun textEstimatorReturnsTimedFramesAndNeutralEnding() {
        val frames = SpeechVisemeTimeline.estimateFromText("Nova", 1f)
        assertTrue(frames.size > 2)
        assertTrue(frames.zipWithNext().all { (a, b) -> a.timeMs <= b.timeMs })
        assertEquals(0f, frames.last().weights["jawOpen"]!!, 0f)
        assertTrue(frames.any { it.weights.containsKey("O") })
        assertTrue(frames.any { it.weights.containsKey("A") })
    }
}
