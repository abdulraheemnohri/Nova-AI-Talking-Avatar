package dev.aitalkingavatar.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarLipSyncControllerTest {
    @Test fun mouthWeightIsZeroWhenNotSpeaking() {
        val controller = AvatarLipSyncController()
        assertEquals(0f, controller.computeMouthWeight(1_000_000_000L, false), 0f)
    }

    @Test fun reducedMotionDisablesApproximateMouthMotion() {
        val controller = AvatarLipSyncController()
        controller.startSpeaking(0L)
        assertEquals(0f, controller.computeMouthWeight(500_000_000L, true), 0f)
    }

    @Test fun mouthWeightStaysWithinExpectedRangeWhileSpeaking() {
        val controller = AvatarLipSyncController()
        controller.startSpeaking(0L)
        for (time in 0L..2_000_000_000L step 50_000_000L) {
            val weight = controller.computeMouthWeight(time, false)
            assertTrue("mouth weight should be >= 0", weight >= 0f)
            assertTrue("mouth weight should be <= 0.75", weight <= 0.75f)
        }
    }

    @Test fun stoppingSpeakingDisablesMouthMotion() {
        val controller = AvatarLipSyncController()
        controller.startSpeaking(0L)
        controller.stopSpeaking()
        assertEquals(0f, controller.computeMouthWeight(250_000_000L, false), 0f)
    }
}
