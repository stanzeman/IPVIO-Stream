package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** Language of the TMDB title information (T-061). */
class MetaLangLogicTest {
    @Test fun emptyChoiceFollowsTheApp() {
        assertEquals("de", MetaLangLogic.effective("", "de"))
        assertEquals("en", MetaLangLogic.effective("  ", "en"))
    }

    @Test fun russianChoiceWinsOverTheAppLanguage() {
        assertEquals("ru", MetaLangLogic.effective("ru", "de"))
        assertEquals("ru-RU", MetaLangLogic.tmdbTag(MetaLangLogic.effective("RU", "de")))
    }

    @Test fun unknownChoiceFallsBackToTheApp() = assertEquals("de", MetaLangLogic.effective("xx", "de"))

    @Test fun tagsOfTheAppLanguagesStayAsBefore() {
        assertEquals("de-DE", MetaLangLogic.tmdbTag("de"))
        assertEquals("en-US", MetaLangLogic.tmdbTag("en"))
        assertEquals("sr-RS", MetaLangLogic.tmdbTag("sr"))
        assertEquals("de-DE", MetaLangLogic.tmdbTag("??"))
    }

    @Test fun everyChoiceHasAName() {
        assertEquals(MetaLangLogic.CODES.size, MetaLangLogic.NAMES.size)
        assertEquals(0, MetaLangLogic.index(""))
        assertEquals(1, MetaLangLogic.index("ru"))
        assertEquals(0, MetaLangLogic.index("zz"))
    }
}
