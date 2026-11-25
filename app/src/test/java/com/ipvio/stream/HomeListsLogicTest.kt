package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-065, T-066: "Popular & new", "New titles" and "Russian & Kazakh". */
class HomeListsLogicTest {
    private val info = HashMap<String, HomeListsLogic.Info?>()
    private fun t(name: String, score: Double?, votes: Int, newest: String, local: Boolean = false): String {
        info[name] = HomeListsLogic.Info(score, votes, newest, local); return name
    }
    private val get: (String) -> HomeListsLogic.Info? = { info[it] }

    @Test fun weighted_rating_pulls_few_votes_to_the_mean() {
        assertEquals(6.0, HomeListsLogic.weighted(9.0, 0, 6.0), 1e-9)
        assertEquals(7.5, HomeListsLogic.weighted(9.0, 50, 6.0), 1e-9)
        assertTrue(HomeListsLogic.weighted(9.0, 10_000, 6.0) > 8.9)
        assertEquals(0.6, HomeListsLogic.popularity(0), 1e-9)
        assertEquals(1.0, HomeListsLogic.popularity(50_000), 1e-9)
    }

    @Test fun popular_new_prefers_recent_titles_over_old_classics() {
        val old = t("Avatar", 7.6, 30_000, "2009-00-00")
        val items = listOf(old) + (1..12).map { t("New$it", 7.0, 2_000, "2025-00-00") }
        val r = HomeListsLogic.popularNew(items, get, 2026)
        assertEquals(12, r.size)
        assertTrue(old !in r)
    }

    @Test fun popular_new_fills_up_with_older_titles_and_skips_unrated() {
        val a = t("A", 8.0, 5_000, "2026-00-00")
        val b = t("B", 6.0, 100, "2024-03-01")
        val c = t("C", 7.0, 9_000, "2010-00-00")
        val d = t("D", null, 0, "2026-00-00")
        assertEquals(listOf(a, b, c), HomeListsLogic.popularNew(listOf(c, b, a, d), get, 2026))
    }

    @Test fun popular_new_ranks_popular_above_obscure_with_same_rating() {
        val a = t("Obscure", 7.5, 60, "2025-00-00")
        val b = t("Hit", 7.5, 8_000, "2025-00-00")
        assertEquals(listOf(b, a), HomeListsLogic.popularNew(listOf(a, b), get, 2026).take(2))
    }

    @Test fun newest_first_by_key_and_last_added_wins_ties() {
        val a = t("A", null, 0, "2023-00-00")
        val b = t("B", null, 0, "2025-11-02")
        val c = t("C", null, 0, "2025-00-00")
        val d = t("D", null, 0, "")
        val e = t("E", null, 0, "2023-00-00")
        assertEquals(listOf(b, c, e, a), HomeListsLogic.newestFirst(listOf(a, b, c, d, e), get))
    }

    @Test fun local_newest_keeps_only_local_titles_unknown_year_last() {
        val a = t("A", null, 0, "2020-00-00", local = true)
        val b = t("B", null, 0, "2026-00-00")
        val c = t("C", null, 0, "", local = true)
        val d = t("D", null, 0, "2024-05-01", local = true)
        assertEquals(listOf(d, a, c), HomeListsLogic.localNewest(listOf(a, b, c, d), get))
    }
}
