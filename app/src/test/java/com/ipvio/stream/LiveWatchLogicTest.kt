package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Most watched channels (T-070). */
class LiveWatchLogicTest {
    private val day = 86_400_000L
    private val t0 = 1_700_000_000_000L
    private fun e(score: Double, ts: Long = t0) = LiveWatchLogic.Entry(score, ts)

    @Test fun shortViewingsAreNotCounted() {
        assertNull(LiveWatchLogic.add(emptyMap(), "a", 29, t0))
        assertEquals(30.0, LiveWatchLogic.add(emptyMap(), "a", 30, t0)!!["a"]!!.score, 0.001)
    }

    @Test fun viewingsAddUp() {
        val m1 = LiveWatchLogic.add(emptyMap(), "a", 600, t0)!!
        val m2 = LiveWatchLogic.add(m1, "a", 300, t0)!!
        assertEquals(900.0, m2["a"]!!.score, 0.001)
        assertEquals(600.0, m1["a"]!!.score, 0.001)   // input not changed
    }

    @Test fun watchTimeHalvesAfterTheHalfLife() {
        assertEquals(500.0, LiveWatchLogic.decayed(e(1000.0), t0 + 30 * day), 0.01)
        assertEquals(1000.0, LiveWatchLogic.decayed(e(1000.0), t0 - day), 0.01)   // clock went back: no growth
    }

    @Test fun rankingUsesDecayedScore() {
        // "old" had more time two months ago, "new" less but now
        val m = mapOf("old" to e(3000.0, t0), "new" to e(1000.0, t0 + 60 * day))
        assertEquals(listOf("new", "old"), LiveWatchLogic.ranked(m, t0 + 60 * day))
        assertEquals(listOf("old", "new"), LiveWatchLogic.ranked(m, t0))
    }

    @Test fun topSkipsMissingAndFilteredChannels() {
        val m = (1..15).associate { "c$it" to e(it * 100.0) }
        val known = setOf("c15", "c13", "c12", "c11", "c10", "c9", "c8", "c7", "c6", "c5", "c4", "c3")
        val r = LiveWatchLogic.top(m, t0, { id -> id.takeIf { it in known } }, { it != "c12" })
        assertEquals(listOf("c15", "c13", "c11", "c10", "c9", "c8", "c7", "c6", "c5", "c4"), r)
    }

    @Test fun storageIsCapped() {
        var m: Map<String, LiveWatchLogic.Entry> = emptyMap()
        for (i in 1..250) m = LiveWatchLogic.add(m, "c$i", 30L + i, t0)!!
        assertEquals(LiveWatchLogic.MAX_ENTRIES, m.size)
        assertTrue("c250" in m)
        assertTrue("c1" !in m)
    }

    @Test fun serializeAndParseRoundTrip() {
        val m = mapOf("a" to e(123.4, t0), "http://x/1.ts" to e(99.0, t0 + 5))
        assertEquals(m, LiveWatchLogic.parse(LiveWatchLogic.serialize(m)))
    }

    @Test fun parseSkipsBrokenLines() {
        val r = LiveWatchLogic.parse("a\t10\t5\nbroken\nb\tx\t5\nc\t-3\t5\n\t1\t1\nd\t7\t9")
        assertEquals(setOf("a", "d"), r.keys)
        assertTrue(LiveWatchLogic.parse(null).isEmpty())
    }

    @Test fun idsWithTabsAreNotStored() {
        assertNull(LiveWatchLogic.add(emptyMap(), "a\tb", 100, t0))
    }

    @Test fun sessionReportsTheFinishedViewingOnChannelChange() {
        val s = LiveWatchLogic.Session()
        assertNull(s.tick("a", 5))
        assertNull(s.tick("a", 5))
        assertEquals("a" to 10L, s.tick("b", 5))
        assertEquals("b" to 5L, s.finish())
        assertNull(s.finish())
    }
}
