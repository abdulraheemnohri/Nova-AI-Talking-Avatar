package dev.aitalkingavatar.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDemoAdapterTest {
    @Test fun returnsUsefulOfflineGreeting() = runBlocking {
        val reply = OfflineDemoAdapter().reply(emptyList(), "hello", false) {}
        assertTrue(reply.contains("offline demo", ignoreCase = true))
        assertTrue(reply.contains("local model", ignoreCase = true))
    }

    @Test fun codingHelpPreservesARealCodeFence() = runBlocking {
        val reply = OfflineDemoAdapter().reply(emptyList(), "hello", true) {}
        assertTrue(reply.contains("```kotlin"))
        assertTrue(reply.contains("fun greet"))
    }

    @Test fun emitsIncrementalUpdatesForTheStreamingUi() = runBlocking {
        val emitted = mutableListOf<String>()
        val answer = OfflineDemoAdapter().reply(emptyList(), "offline", false) { emitted += it }
        assertTrue(emitted.isNotEmpty())
        assertTrue(emitted.last() == answer)
    }
}
