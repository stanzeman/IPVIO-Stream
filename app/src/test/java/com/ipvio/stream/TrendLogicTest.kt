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
        TrendLogic.rank(items, b, { it.key }, { it.title }, { if (it.local) TrendLogic.LOCAL_BOOST else 1.0 }, limit).map { it.title }

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

    @Test fun focus_only_for_russian_title_info() {
        assertEquals(TrendLogic.FOCUS_RU, TrendLogic.focusFor("ru"))
        assertEquals("", TrendLogic.focusFor("de"))
        assertEquals("", TrendLogic.focusFor(""))
    }

    @Test fun boost_follows_original_language() {
        val f = TrendLogic.FOCUS_RU
        assertEquals(TrendLogic.LOCAL_BOOST, TrendLogic.boost(f, "ru", emptyList()), 0.0)
        // Kazakh production in Russian: full bonus
        assertEquals(TrendLogic.LOCAL_BOOST, TrendLogic.boost(f, "ru", listOf("KZ")), 0.0)
        assertEquals(TrendLogic.LOCAL_BOOST, TrendLogic.boost(f, "kk", emptyList()), 0.0)
        assertEquals(TrendLogic.CIS_BOOST, TrendLogic.boost(f, "uk", listOf("UA")), 0.0)
        // Shot in the region, but in English (e.g. "Rise of the Conqueror"): only the small country bonus
        assertEquals(TrendLogic.COUNTRY_BOOST, TrendLogic.boost(f, "en", listOf("UZ", "US")), 0.0)
        assertEquals(TrendLogic.COUNTRY_BOOST, TrendLogic.boost(f, "en", listOf("KZ")), 0.0)
        assertEquals(1.0, TrendLogic.boost(f, "en", listOf("US")), 0.0)
        assertTrue(TrendLogic.LOCAL_BOOST > TrendLogic.CIS_BOOST && TrendLogic.CIS_BOOST > TrendLogic.COUNTRY_BOOST)
        assertTrue(TrendLogic.W_RU > TrendLogic.W_CIS && TrendLogic.W_CIS > TrendLogic.W_GLOBAL)
    }

    @Test fun region_lists_by_language_only_with_focus() {
        assertTrue(TrendLogic.regionLists("").isEmpty())
        val l = TrendLogic.regionLists(TrendLogic.FOCUS_RU)
        assertEquals("ru", l[0].first); assertEquals(TrendLogic.W_RU, l[0].second, 0.0)
        assertEquals("kk", l[1].first); assertEquals(TrendLogic.W_KZ, l[1].second, 0.0)
        assertTrue(l.drop(2).all { it.second == TrendLogic.W_CIS && "|" !in it.first })
        assertTrue(l.none { it.first == "en" })
    }

    @Test fun no_boost_without_focus() {
        assertEquals(1.0, TrendLogic.boost("", "ru", listOf("RU")), 0.0)
        assertEquals(1.0, TrendLogic.boost("", "uk", listOf("UA")), 0.0)
    }

    @Test fun board_of_other_focus_is_not_fresh() {
        val b = TrendLogic.Board("2026-11-25", 1_000L, mapOf("m1" to 1.0), emptyMap(), TrendLogic.FOCUS_RU)
        assertTrue(TrendLogic.fresh(b, "2026-11-25", TrendLogic.FOCUS_RU))
        assertFalse(TrendLogic.fresh(b, "2026-11-25", ""))
        assertTrue(TrendLogic.usable(b, 2_000L))
    }
}
