package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-064: "Top 10 today" from worldwide trends with extra weight on Russia and Kazakhstan. */
class TrendLogicTest {
    private class T(val key: String?, val title: String, val local: Boolean = false)

    private fun board(points: Map<String, Double>, titles: Map<String, String> = emptyMap()) =
        TrendLogic.Board("2026-11-25", 1_000L, points, titles)

    private fun rank(items: List<T>, b: TrendLogic.Board, limit: Int = 10) =
        TrendLogic.rank(items, b, { it.key }, { it.title }, { it.local }, limit).map { it.title }

    @Test fun fuse_adds_up_titles_in_several_lists_and_weights_russia_higher() {
        val p = TrendLogic.fuse(listOf(
            TrendLogic.Source(TrendLogic.W_GLOBAL, listOf("m1", "m2")),
            TrendLogic.Source(TrendLogic.W_RU, listOf("m3", "m1"))
        ))
        assertTrue(p.getValue("m1") > p.getValue("m3"))
        // Place 1 in the Russian list beats place 1 worldwide
        assertTrue(p.getValue("m3") > TrendLogic.W_GLOBAL / (TrendLogic.K + 1))
        assertEquals(TrendLogic.W_KZ, TrendLogic.W_RU, 0.0)
    }

    @Test fun fuse_counts_a_key_once_per_list() {
        val p = TrendLogic.fuse(listOf(TrendLogic.Source(1.0, listOf("m1", "m1"))))
        assertEquals(1.0 / (TrendLogic.K + 1), p.getValue("m1"), 1e-9)
    }

    @Test fun rank_orders_by_points_and_matches_by_title_when_id_unknown() {
        val b = board(mapOf("m1" to 0.03, "m2" to 0.05), mapOf("dune" to "m1"))
        val r = rank(listOf(T("m2", "B"), T(null, "Dune!"), T("m9", "X")), b)
        assertEquals(listOf("B", "Dune!"), r)
    }

    @Test fun rank_boosts_local_titles_and_keeps_one_entry_per_key() {
        val b = board(mapOf("m1" to 0.050, "m2" to 0.045))
        val r = rank(listOf(T("m1", "A"), T("m2", "Brat", local = true), T("m2", "Brat HD", local = true)), b)
        assertEquals(listOf("Brat", "A"), r)
    }

    @Test fun empty_board_gives_empty_list_and_limit_applies() {
        assertTrue(rank(listOf(T("m1", "A")), TrendLogic.EMPTY).isEmpty())
        val b = board((1..15).associate { "m$it" to it.toDouble() })
        assertEquals(10, rank((1..15).map { T("m$it", "t$it") }, b).size)
    }

    @Test fun fresh_and_usable() {
        val b = board(mapOf("m1" to 1.0))
        assertTrue(TrendLogic.fresh(b, "2026-11-25"))
        assertFalse(TrendLogic.fresh(b, "2026-11-26"))
        assertTrue(TrendLogic.usable(b, 1_000L + TrendLogic.MAX_AGE_MS))
        assertFalse(TrendLogic.usable(b, 1_000L + TrendLogic.MAX_AGE_MS + 1))
        assertFalse(TrendLogic.usable(b, 999L))
        assertFalse(TrendLogic.fresh(TrendLogic.EMPTY, ""))
    }

    @Test fun local_means_russian_or_kazakh() {
        assertTrue(TrendLogic.isLocal("ru", emptyList()))
        assertTrue(TrendLogic.isLocal("kk", emptyList()))
        assertTrue(TrendLogic.isLocal("en", listOf("US", "KZ")))
        assertFalse(TrendLogic.isLocal("uk", listOf("UA")))
        assertFalse(TrendLogic.isLocal(null, emptyList()))
        assertEquals("t42", TrendLogic.key(true, 42))
    }
}
