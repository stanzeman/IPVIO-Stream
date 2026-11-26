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

    private fun rank(items: List<T>, b: TrendLogic.Board, limit: Int = 10, tv: Boolean = false) =
        TrendLogic.rank(items, b, tv, { it.key }, { it.title }, { if (it.local) TrendLogic.LOCAL_BOOST else 1.0 }, limit).map { it.title }

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
        val b = board(mapOf("m1" to 0.03, "m2" to 0.05), mapOf(TrendLogic.titleKey(false, "Dune") to "m1"))
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

    // T-073: the TMDB number decides, the name only helps while it is unknown

    @Test fun known_number_not_on_board_does_not_fall_back_to_the_name() {
        // "Roots" (t100) stored for the playlist title "Корни"; the trend series "Корни" is t200
        val b = board(mapOf("t200" to 0.05), mapOf(TrendLogic.titleKey(true, "Корни") to "t200"))
        assertTrue(rank(listOf(T("t100", "Корни")), b, tv = true).isEmpty())
        assertEquals(listOf("Корни"), rank(listOf(T(null, "Корни")), b, tv = true))
    }

    @Test fun name_fallback_stays_within_the_same_kind() {
        // The movie "Мастер и Маргарита" (2024) is trending; the series of 2005 with the same name must not take its points
        val b = board(mapOf("m300" to 0.05), mapOf(TrendLogic.titleKey(false, "Мастер и Маргарита") to "m300"))
        assertTrue(rank(listOf(T(null, "Мастер и Маргарита")), b, tv = true).isEmpty())
        assertEquals(listOf("Мастер и Маргарита"), rank(listOf(T(null, "Мастер и Маргарита")), b, tv = false))
        assertTrue(TrendLogic.titleKey(true, "A") != TrendLogic.titleKey(false, "A"))
    }

    @Test fun candidates_are_unknown_or_differently_matched_titles_best_first() {
        val b = board(
            mapOf("t1" to 0.02, "t2" to 0.05, "t3" to 0.04),
            mapOf(TrendLogic.titleKey(true, "Корни") to "t2", TrendLogic.titleKey(true, "Люпен") to "t1", TrendLogic.titleKey(true, "Тверская") to "t3")
        )
        val items = listOf(T(null, "Люпен"), T("t100", "Корни"), T("t3", "Тверская"), T(null, "Нет в тренде"))
        val c = TrendLogic.candidates(items, b, true, { it.key }, { it.title }, 10).map { it.title }
        // "Тверская" already has the trend number: no check; unrelated titles are left out
        assertEquals(listOf("Корни", "Люпен"), c)
        assertEquals(1, TrendLogic.candidates(items, b, true, { it.key }, { it.title }, 1).size)
        assertTrue(TrendLogic.candidates(items, b, false, { it.key }, { it.title }, 10).isEmpty())
    }

    @Test fun board_of_other_language_is_not_fresh() {
        val b = TrendLogic.Board("2026-11-25", 1_000L, mapOf("m1" to 1.0), emptyMap(), "", "ru-RU")
        assertTrue(TrendLogic.fresh(b, "2026-11-25", "", "ru-RU"))
        assertFalse(TrendLogic.fresh(b, "2026-11-25", "", "de-DE"))
        // A board saved before T-073 has no language: loaded again once
        assertFalse(TrendLogic.fresh(board(mapOf("m1" to 1.0)), "2026-11-25", "", "ru-RU"))
        val r = b.renewed()
        assertTrue(r !== b && r.points == b.points && r.lang == b.lang)
    }

    @Test fun series_with_season_in_the_name_matches_the_trend_title() {
        // T-074: the caller passes the search title (without "Сезон N")
        val b = board(mapOf("t46080" to 0.06), mapOf(TrendLogic.titleKey(true, "Маша и Медведь") to "t46080"))
        val r = TrendLogic.rank(listOf(T(null, "Маша и Медведь. Сезон 5.")), b, true, { it.key }, { MatchLogic.searchTitle(it.title) }, { 1.0 }, 10)
        assertEquals(1, r.size)
    }

    @Test fun newest_season_shows_when_several_seasons_match() {
        val b = board(mapOf("t1" to 0.06), mapOf(TrendLogic.titleKey(true, "Невский") to "t1"))
        val items = listOf(T(null, "Невский. Сезон 1"), T(null, "Невский. Сезон 5."), T(null, "Невский. Сезон 3"))
        val r = TrendLogic.rank(items, b, true, { it.key }, { MatchLogic.searchTitle(it.title) }, { 1.0 }, 10) { MatchLogic.seasonOf(it.title) }
        assertEquals(listOf("Невский. Сезон 5."), r.map { it.title })
        // Without season numbers the first entry stays
        val r2 = TrendLogic.rank(listOf(T(null, "Невский"), T(null, "Невский!")), b, true, { it.key }, { MatchLogic.searchTitle(it.title) }, { 1.0 }, 10) { MatchLogic.seasonOf(it.title) }
        assertEquals(listOf("Невский"), r2.map { it.title })
    }

    @Test fun animation_by_tmdb_genre_or_provider_category() {
        // T-076
        assertTrue(TrendLogic.isAnimation(listOf(16, 10751), "сериалы"))
        assertTrue(TrendLogic.isAnimation(emptyList(), "мультфильмы"))
        assertTrue(TrendLogic.isAnimation(emptyList(), "мультсериалы"))
        assertTrue(TrendLogic.isAnimation(emptyList(), "Аниме"))
        assertTrue(TrendLogic.isAnimation(emptyList(), "DE | Zeichentrick"))
        assertFalse(TrendLogic.isAnimation(listOf(18, 80), "сериалы"))
        assertFalse(TrendLogic.isAnimation(emptyList(), "фильмы"))
        assertFalse(TrendLogic.isAnimation(emptyList(), null))
    }

    @Test fun verifyKey_is_the_same_for_the_same_board_and_list_size() {
        val b = TrendLogic.Board("2026-11-26", 1L, mapOf("m:1" to 1.0), emptyMap(), "ru", "ru-RU")
        assertEquals(TrendLogic.verifyKey(b, 500), TrendLogic.verifyKey(b.renewed(), 500))
    }

    @Test fun verifyKey_changes_with_day_language_focus_and_list_size() {
        val b = TrendLogic.Board("2026-11-26", 1L, mapOf("m:1" to 1.0), emptyMap(), "ru", "ru-RU")
        val k = TrendLogic.verifyKey(b, 500)
        assertFalse(k == TrendLogic.verifyKey(b, 501))
        assertFalse(k == TrendLogic.verifyKey(TrendLogic.Board("2026-11-27", 1L, b.points, b.titles, "ru", "ru-RU"), 500))
        assertFalse(k == TrendLogic.verifyKey(TrendLogic.Board("2026-11-26", 1L, b.points, b.titles, "ru", "de-DE"), 500))
        assertFalse(k == TrendLogic.verifyKey(TrendLogic.Board("2026-11-26", 1L, b.points, b.titles, "", "ru-RU"), 500))
    }
}
