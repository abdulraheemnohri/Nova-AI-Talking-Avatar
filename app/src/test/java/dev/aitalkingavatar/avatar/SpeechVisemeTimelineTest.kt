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
    @Test fun longTextEstimateCoversFullTtsInputLimit() {
        val text = "a".repeat(1200)
        val frames = SpeechVisemeTimeline.estimateFromText(text, 1f)
        assertTrue(frames.size > 2000)
        assertTrue(frames.last().timeMs >= 1200L * 24L)
        assertEquals(0f, frames.last().weights["jawOpen"]!!, 0f)
    }

    @Test fun punctuationAddsNeutralPauseInsteadOfInventingMouthShapes() {
        val plain = SpeechVisemeTimeline.estimateFromText("a", 1f)
        val punctuated = SpeechVisemeTimeline.estimateFromText("a.", 1f)
        assertTrue(punctuated.last().timeMs > plain.last().timeMs + 150L)
        assertEquals(0f, punctuated.last().weights["jawOpen"]!!, 0f)
        assertTrue(punctuated.all { frame -> frame.weights.keys.all { it == "jawOpen" || it == "A" } })
    }

    @Test fun commasAndSentenceEndsHaveDifferentPauseLengths() {
        val comma = SpeechVisemeTimeline.estimateFromText("a,", 1f).last().timeMs
        val sentenceEnd = SpeechVisemeTimeline.estimateFromText("a.", 1f).last().timeMs
        assertTrue(sentenceEnd > comma + 80L)
    }

    @Test fun emptyTimelineSamplesAsEmptyMap() {
        assertEquals(emptyMap<String, Float>(), SpeechVisemeTimeline(emptyList()).sample(0L))
    }

    @Test fun negativeFrameTimesAreNormalizedToZero() {
        val timeline = SpeechVisemeTimeline(listOf(
            VisemeFrame(-100L, mapOf("jawOpen" to 0.25f)),
            VisemeFrame(50L, mapOf("jawOpen" to 0.75f))
        ))
        assertEquals(0L, timeline.frames.first().timeMs)
        assertEquals(0.25f, timeline.sample(0L)["jawOpen"]!!, 0f)
    }

    @Test fun absentMorphWeightsFadeToNeutralDuringInterpolation() {
        val timeline = SpeechVisemeTimeline(listOf(
            VisemeFrame(0L, mapOf("jawOpen" to 1f, "A" to 0.8f)),
            VisemeFrame(100L, mapOf("jawOpen" to 0f))
        ))
        val middle = timeline.sample(50L)
        assertEquals(0.5f, middle["jawOpen"]!!, 0.0001f)
        assertEquals(0.4f, middle["A"]!!, 0.0001f)
        assertEquals(0f, timeline.sample(100L)["A"] ?: 0f, 0f)
    }

    @Test fun estimatorClampsSpeechRateAndBoundsVeryLongInput() {
        val slow = SpeechVisemeTimeline.estimateFromText("a", 0f)
        val minimumRate = SpeechVisemeTimeline.estimateFromText("a", 0.5f)
        assertEquals(minimumRate.last().timeMs, slow.last().timeMs)

        val fast = SpeechVisemeTimeline.estimateFromText("a", 99f)
        val maximumRate = SpeechVisemeTimeline.estimateFromText("a", 2f)
        assertEquals(maximumRate.last().timeMs, fast.last().timeMs)

        val longFrames = SpeechVisemeTimeline.estimateFromText("a".repeat(4000))
        assertEquals(
            SpeechVisemeTimeline.estimateFromText("a".repeat(3500)).last().timeMs,
            longFrames.last().timeMs
        )
    }

}
