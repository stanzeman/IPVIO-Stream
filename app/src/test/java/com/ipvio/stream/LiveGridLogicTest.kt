package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** Live TV as tiles (T-060). */
class LiveGridLogicTest {
    private fun t(k: String, n: Int) = LiveGridLogic.Tile(k, k, n)
    private fun ch(id: String, logo: String?) = Item(id, Kind.LIVE, id, null, "http://h/$id", logo, "G")

    @Test fun tilesFollowTheListOrderAndLeaveOutEmptyOnes() {
        val r = LiveGridLogic.tiles(3, listOf(t("L:1", 2)), 100, listOf(t("g_sport", 5), t("g_kids", 0)), listOf(t("Кино", 9)), listOf(t("Общие", 20), t("Кино", 9), t("Leer", 0)))
        assertEquals(listOf(FAV_CAT, "L:1", "ALL", "g_sport", "Кино", "Общие"), r.map { it.key })
    }

    @Test fun noFavoritesTileWithoutFavorites() {
        val r = LiveGridLogic.tiles(0, emptyList(), 5, emptyList(), emptyList(), listOf(t("A", 5)))
        assertEquals(listOf("ALL", "A"), r.map { it.key })
    }

    @Test fun previewTakesUpToFourDifferentLogos() {
        val l = listOf(ch("a", "x.png"), ch("b", null), ch("c", "x.png"), ch("d", "y.png"), ch("e", "z.png"), ch("f", "w.png"), ch("g", "v.png"))
        assertEquals(listOf("x.png", "y.png", "z.png", "w.png"), LiveGridLogic.preview(l, { it.logo }))
    }

    @Test fun progressStaysBetweenZeroAndOne() {
        assertEquals(0.5f, LiveGridLogic.progress(150, 100, 200), 0.001f)
        assertEquals(0f, LiveGridLogic.progress(50, 100, 200), 0.001f)
        assertEquals(1f, LiveGridLogic.progress(500, 100, 200), 0.001f)
        assertEquals(0f, LiveGridLogic.progress(150, 200, 200), 0.001f)
    }

    @Test fun indexForComingBackFromThePlayer() {
        val l = listOf(ch("a", null), ch("b", null))
        assertEquals(1, LiveGridLogic.indexOf(l, "b"))
        assertEquals(-1, LiveGridLogic.indexOf(l, "x"))
        assertEquals(-1, LiveGridLogic.indexOf(l, null))
    }

    @Test fun initialsOfAChannelWithoutLogo() {
        assertEquals("KA", LiveGridLogic.initials("Kabel Eins DE"))
        assertEquals("ПЕ", LiveGridLogic.initials("Первый канал"))
        assertEquals("TV", LiveGridLogic.initials(" - "))
    }
}
