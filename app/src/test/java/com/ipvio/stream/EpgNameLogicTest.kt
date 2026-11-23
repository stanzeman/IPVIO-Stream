package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Guide channel by name for playlists without tvg-id (T-053). */
class EpgNameLogicTest {
    @Test
    fun qualityWordsAndCaseDoNotChangeTheKey() {
        assertEquals("первый канал", EpgNameLogic.key("Первый канал HD"))
        assertEquals("первый канал", EpgNameLogic.key("ПЕРВЫЙ  КАНАЛ UHD"))
        assertEquals("первый канал", EpgNameLogic.key("Первый канал"))
        assertEquals("ntv", EpgNameLogic.key("NTV 4K"))
    }

    @Test
    fun yoCountsAsYe() {
        assertEquals(EpgNameLogic.key("Звёзды"), EpgNameLogic.key("Звезды"))
    }

    @Test
    fun timeShiftStaysPartOfTheName() {
        assertEquals("нтв +2", EpgNameLogic.key("НТВ +2"))
        assertEquals("нтв +2", EpgNameLogic.key("НТВ HD +2"))
        assertEquals(false, EpgNameLogic.key("НТВ +2") == EpgNameLogic.key("НТВ"))
    }

    @Test
    fun punctuationBecomesASpace() {
        assertEquals("match премьер", EpgNameLogic.key("Match! Премьер"))
        assertEquals("пятница", EpgNameLogic.key("Пятница!"))
        assertEquals("sky sport 1 de", EpgNameLogic.key("Sky Sport 1 FHD DE"))
    }

    @Test
    fun nameWithoutLettersGivesAnEmptyKey() {
        assertEquals("", EpgNameLogic.key(""))
        assertEquals("", EpgNameLogic.key(" - HD "))
    }

    @Test
    fun theGuideChannelWithTheMostShowsWins() {
        val shows = mapOf("2036" to 0, "162" to 349, "999" to 12)
        assertEquals("162", EpgNameLogic.pick(listOf("2036", "162", "999")) { shows[it] ?: 0 })
    }

    @Test
    fun equalShowCountsGoToTheSmallerId() {
        assertEquals("a1", EpgNameLogic.pick(listOf("b2", "a1")) { 10 })
    }

    @Test
    fun noChannelWithShowsGivesNull() {
        assertNull(EpgNameLogic.pick(listOf("1", "2")) { 0 })
        assertNull(EpgNameLogic.pick(emptyList()) { 5 })
    }
}
