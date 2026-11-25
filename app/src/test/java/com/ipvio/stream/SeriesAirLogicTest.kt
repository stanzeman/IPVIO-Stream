package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Date of the latest episode (T-063). */
class SeriesAirLogicTest {
    private val day = 24 * 3_600_000L
    private val now = 1000 * day

    @Test fun neverAskedIsDue() = assertTrue(SeriesAirLogic.due(null, 0L, now, 2026))

    @Test fun runningSeriesAreAskedAgainAfterSevenDays() {
        assertFalse(SeriesAirLogic.due("2026-11-20", now - 6 * day, now, 2026))
        assertTrue(SeriesAirLogic.due("2026-11-20", now - 7 * day, now, 2026))
        assertTrue(SeriesAirLogic.due("2025-03-01", now - 8 * day, now, 2026))
    }

    @Test fun oldSeriesOnlyAfter90Days() {
        assertFalse(SeriesAirLogic.due("2019-05-01", now - 60 * day, now, 2026))
        assertTrue(SeriesAirLogic.due("2019-05-01", now - 90 * day, now, 2026))
    }

    @Test fun clockInThePastAsksAgain() = assertTrue(SeriesAirLogic.due("2026-01-01", now + day, now, 2026))

    @Test fun newestKeyPrefersTheLatestEpisodeForSeries() {
        assertEquals("2026-11-20", SeriesAirLogic.newestKey(true, "2026-11-20", "2019"))
        assertEquals("2019-00-00", SeriesAirLogic.newestKey(true, null, "2019"))
        assertEquals("2024-00-00", SeriesAirLogic.newestKey(false, "2026-11-20", "2024"))
        assertEquals("", SeriesAirLogic.newestKey(false, null, null))
        assertTrue(SeriesAirLogic.newestKey(true, "2026-01-05", "2026") > SeriesAirLogic.newestKey(false, null, "2026"))
    }

    @Test fun yearOfDateOrYear() {
        assertEquals(2026, SeriesAirLogic.yearOf("2026-11-20"))
        assertEquals(2019, SeriesAirLogic.yearOf("2019"))
        assertNull(SeriesAirLogic.yearOf("abc"))
    }
}
