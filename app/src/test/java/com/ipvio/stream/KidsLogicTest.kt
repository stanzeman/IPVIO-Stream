package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidsLogicTest {
    @Test fun providerCategoriesDecideCartoonOrAnime() {
        assertEquals(KidsLogic.CARTOON, KidsLogic.classify(emptyList(), null, "мультфильмы"))
        assertEquals(KidsLogic.CARTOON, KidsLogic.classify(emptyList(), null, "Мультсериалы"))
        assertEquals(KidsLogic.ANIME, KidsLogic.classify(emptyList(), null, "Аниме"))
        assertEquals(KidsLogic.ANIME, KidsLogic.classify(emptyList(), "en", "Anime Series"))
        assertEquals(KidsLogic.CARTOON, KidsLogic.classify(emptyList(), null, "Dessins animés"))
        assertEquals(KidsLogic.NONE, KidsLogic.classify(emptyList(), null, "Комедии"))
        assertEquals(KidsLogic.NONE, KidsLogic.classify(listOf(35, 18), "ru", null))
    }

    @Test fun tmdbAnimationIsAnimeOnlyWithJapaneseOriginalLanguage() {
        assertEquals(KidsLogic.ANIME, KidsLogic.classify(listOf(16, 10759), "ja", "Фильмы"))
        assertEquals(KidsLogic.CARTOON, KidsLogic.classify(listOf(16), "ru", null))
        assertEquals(KidsLogic.CARTOON, KidsLogic.classify(listOf(16, 10751), "en", "Kids"))
        // Japanese live action is no anime
        assertEquals(KidsLogic.NONE, KidsLogic.classify(listOf(18), "ja", null))
        assertTrue(KidsLogic.isAnimeGroup("ANIME 4K"))
        assertFalse(KidsLogic.isAnimeGroup(null))
    }

    @Test fun topTakesTrendThenPopularThenNewestEachOnce() {
        val r = KidsLogic.top(listOf("a", "b"), listOf("b", "c", "d"), listOf("e", "a", "f"), { it }, 5)
        assertEquals(listOf("a", "b", "c", "d", "e"), r)
        assertEquals(listOf("e", "f"), KidsLogic.top(emptyList(), emptyList(), listOf("e", "f"), { it }, 10))
    }

    @Test fun heroMixAlternatesRowsWithoutDuplicates() {
        val r = KidsLogic.heroMix(listOf(listOf("a", "b"), listOf("x", "a"), emptyList(), listOf("z")), { it }, 2)
        assertEquals(listOf("a", "x", "z", "b"), r)
    }
}
