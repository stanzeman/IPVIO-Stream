package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-072: when a title enters, finishes in and leaves "Continue watching". */
class ResumeRulesT072Test {
    private val min = 60_000L
    private val day = 86_400_000L

    @Test fun appearsOnlyAfterFiveMinutes() {
        assertFalse(ResumeLogic.shouldSave(false, 0, 0, 0, 0, 3 * min, false))
        assertFalse(ResumeLogic.shouldSave(false, 0, 0, 0, 0, 5 * min, false))
        assertTrue(ResumeLogic.shouldSave(false, 0, 0, 0, 0, 5 * min + 1, false))
        // going on later in a title already past 5 minutes is always remembered (absolute position)
        assertTrue(ResumeLogic.shouldSave(true, 0, 0, 0, 0, 43 * min, false))
    }

    @Test fun finishedFromNinetyPercent() {
        val film = 120 * min
        assertFalse(ResumeLogic.finished(107 * min, film))    // 89 %
        assertTrue(ResumeLogic.finished(108 * min, film))     // 90 %: long credits are covered
    }

    @Test fun finishedInTheLastThreeMinutes() {
        val ep = 24 * min
        assertTrue(ResumeLogic.finished(21 * min, ep))        // 3 minutes left
        assertFalse(ResumeLogic.finished(20 * min, ep))       // 4 minutes left, 83 %
        // a very short clip: the last 3 minutes only count once half has played
        assertFalse(ResumeLogic.finished(1 * min, 4 * min))
        assertTrue(ResumeLogic.finished(2 * min, 4 * min))
    }

    @Test fun expiresAfterTheSetDays() {
        val now = 100 * day
        assertFalse(ResumeLogic.expired(now - 30 * day, now, 0))        // never
        assertFalse(ResumeLogic.expired(now - 3 * day + 1, now, 3))
        assertTrue(ResumeLogic.expired(now - 3 * day - 1, now, 3))
        assertFalse(ResumeLogic.expired(now + day, now, 3))             // clock went back
        assertFalse(ResumeLogic.expired(0, now, 3))                     // no time stamp
    }

    @Test fun dayChoices() {
        assertEquals(listOf(0, 3, 7, 14, 30), ResumeLogic.EXPIRE_DAYS)
    }
}
